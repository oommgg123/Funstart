package moe.hinakusoft.funstart.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.FunstartPlugin.PendingChatAction;
import moe.hinakusoft.funstart.FunstartPlugin.TpaResponseData;
import moe.hinakusoft.funstart.manager.BackTeleportUtil;
import moe.hinakusoft.funstart.manager.TpaManager;
import moe.hinakusoft.funstart.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;


public class TpagGuiListener implements Listener {

    private final FunstartPlugin plugin;

    public TpagGuiListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- Open methods ----

    public static void openFor(Player player, FunstartPlugin plugin, boolean isHere) {
        Inventory inv = Bukkit.createInventory(new TpagHolder(player, isHere, TpagHolder.View.MAIN), 54,
            isHere ? "§8\uD83D\uDCE9 TPAH - 邀请传送" : "§8\uD83D\uDCE9 TPA - 请求传送");

        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (slot >= 45) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setPlayerProfile(online.getPlayerProfile());
            meta.setDisplayName("§b" + online.getName());
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        inv.setItem(45, makeItem(Material.GREEN_CONCRETE, "§a§l同意传送请求",
            "§7点击同意当前传送请求"));

        inv.setItem(46, makeItem(Material.SKELETON_SKULL, "§8§l回到死亡点",
            "§7传送至上次死亡地点 (跨纬度)", "§e消耗: 5 点"));

        inv.setItem(47, makeItem(Material.RED_BED, "§c§l回到重生点",
            "§7传送至你的床重生点", "§e消耗: 3 点"));

        inv.setItem(48, makeItem(Material.DIRT, "§a§l回到主城",
            "§7传送至主城出生点", "§e消耗: 3 点"));

        int pendingCount = plugin.getTpaManager().getRequestsByTarget(player.getUniqueId()).size();
        inv.setItem(49, makeItem(Material.BOOK, "§d§l待处理的请求",
            "§7当前有 §e" + pendingCount + " §7个待处理的请求", "§e点击查看"));

        inv.setItem(53, makeItem(Material.BARRIER, "§c§l拒绝传送请求",
            "§7点击拒绝当前传送请求"));

        player.openInventory(inv);
    }

    public static void openPendingRequests(Player player, FunstartPlugin plugin) {
        List<TpaManager.TpaRequest> requests = plugin.getTpaManager().getRequestsByTarget(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(
            new TpagHolder(player, false, TpagHolder.View.PENDING), 54,
            "§d待处理的传送请求");

        int slot = 0;
        for (TpaManager.TpaRequest req : requests) {
            if (slot + 3 > 54) break;
            UUID fromUuid = req.getRequester();
            String fromName = Bukkit.getOfflinePlayer(fromUuid).getName();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setPlayerProfile(Bukkit.getOfflinePlayer(fromUuid).getPlayerProfile());
            String typeStr = req.getType() == TpaManager.TpaType.TPAH ? "邀请你" : "请求传送";
            sm.setDisplayName("§b" + (fromName != null ? fromName : "未知"));
            sm.setLore(List.of("§7" + typeStr));
            head.setItemMeta(sm);
            inv.setItem(slot++, head);

            inv.setItem(slot++, makeItem(Material.GREEN_CONCRETE, "§a§l接受",
                "§7接受 §b" + (fromName != null ? fromName : "未知") + " §7的请求"));
            inv.setItem(slot++, makeItem(Material.RED_CONCRETE, "§c§l拒绝",
                "§7拒绝 §b" + (fromName != null ? fromName : "未知") + " §7的请求"));
        }

        inv.setItem(49, makeItem(Material.ARROW, "§a返回", "§7返回上一页"));
        inv.setItem(53, makeItem(Material.BARRIER, "§c关闭"));

        player.openInventory(inv);
    }

    // ---- Event handlers ----

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TpagHolder holder)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (holder.getView() == TpagHolder.View.PENDING) {
            handlePendingClick(player, slot);
            return;
        }

        // MAIN view
        if (slot == 45) {
            handleAccept(player);
            return;
        }
        if (slot == 46) {
            handleBackdie(player);
            return;
        }
        if (slot == 47) {
            handleBackhome(player);
            return;
        }
        if (slot == 48) {
            handleBackworld(player);
            return;
        }
        if (slot == 49) {
            openPendingRequests(player, plugin);
            return;
        }
        if (slot == 53) {
            handleReject(player);
            return;
        }
        if (slot < 45) {
            handlePlayerClick(player, slot, holder.isHere(), event.getInventory());
        }
    }

    private void handlePlayerClick(Player player, int slot, boolean isHere, Inventory inv) {
        ItemStack item = inv.getItem(slot);
        if (item == null || !item.hasItemMeta()) return;
        String targetName = item.getItemMeta().getDisplayName();
        if (targetName == null || !targetName.startsWith("§b")) {
            player.sendMessage("§c无法识别此操作");
            return;
        }
        targetName = targetName.substring(2);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c目标玩家已离线");
            return;
        }
        if (target.equals(player)) return;

        TpaManager tpaManager = plugin.getTpaManager();
        if (tpaManager.hasPendingRequest(player.getUniqueId())) {
            player.sendMessage("§c你已有一个待处理的传送请求或邀请");
            return;
        }

        TpaManager.TpaType type = isHere ? TpaManager.TpaType.TPAH : TpaManager.TpaType.TPA;

        tpaManager.createRequest(player.getUniqueId(), target.getUniqueId(), type, -1);

        if (isHere) {
            player.sendMessage("§e[Funstart] §a已邀请 §b" + target.getName() + " §a传送到你身边");
            target.sendMessage("§e[Funstart] §b" + player.getName() + " §a邀请你传送到他身边!");
        } else {
            player.sendMessage("§e[Funstart] §a已向 §b" + target.getName() + " §a发送传送请求");
            target.sendMessage("§e[Funstart] §b" + player.getName() + " §a请求传送到你身边!");
        }
        plugin.addPendingChatAction(target.getUniqueId(), PendingChatAction.Type.TPA_RESPONSE, new TpaResponseData(player.getUniqueId(), type));
        target.sendMessage("§7输入 §a1 §7同意, §c2 §7拒绝 (§760秒内)");
        player.closeInventory();
    }

    private void handlePendingClick(Player player, int slot) {
        if (slot == 49) {
            openFor(player, plugin, false);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        if (slot < 0 || slot >= 54) return;

        List<TpaManager.TpaRequest> requests = plugin.getTpaManager().getRequestsByTarget(player.getUniqueId());
        int reqIdx = slot / 3;
        int subSlot = slot % 3;

        if (reqIdx >= requests.size()) return;
        TpaManager.TpaRequest req = requests.get(reqIdx);
        UUID requesterUuid = req.getRequester();
        String fromName = Bukkit.getOfflinePlayer(requesterUuid).getName();

        if (subSlot == 0) return; // head slot, no action

        if (subSlot == 1) { // accept
            Player requester = Bukkit.getPlayer(requesterUuid);
            if (requester == null || !requester.isOnline()) {
                player.sendMessage("§c对方已离线");
                plugin.getTpaManager().removeRequest(requesterUuid);
                Bukkit.getScheduler().cancelTask(req.getTaskId());
                openPendingRequests(player, plugin);
                return;
            }
            Bukkit.getScheduler().cancelTask(req.getTaskId());
            plugin.getTpaManager().removeRequest(requesterUuid);

            if (req.getType() == TpaManager.TpaType.TPAH) {
                player.teleportAsync(requester.getLocation());
                player.sendMessage("§e[Funstart] §a已传送到 §b" + requester.getName() + " §a身边");
                requester.sendMessage("§e[Funstart] §b" + player.getName() + " §a已接受邀请, 传送至你身边");
            } else {
                requester.teleportAsync(player.getLocation());
                player.sendMessage("§e[Funstart] §a已同意传送请求");
                requester.sendMessage("§e[Funstart] §a已传送至 §b" + player.getName() + " §a身边");
            }
            openPendingRequests(player, plugin);
        } else if (subSlot == 2) { // reject
            plugin.getTpaManager().removeRequest(requesterUuid);
            Bukkit.getScheduler().cancelTask(req.getTaskId());
            player.sendMessage("§c已拒绝 §b" + (fromName != null ? fromName : "未知"));
            Player requester = Bukkit.getPlayer(requesterUuid);
            if (requester != null && requester.isOnline()) {
                requester.sendMessage("§e[Funstart] §b" + player.getName() + " §c已拒绝");
            }
            openPendingRequests(player, plugin);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TpagHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {}

    // ---- Accept/Reject/Back actions ----

    private void handleAccept(Player player) {
        TpaManager.TpaRequest request = plugin.getTpaManager().getRequestByTarget(player.getUniqueId());
        if (request == null) {
            player.sendMessage("§c没有待处理的传送请求");
            return;
        }
        Player requester = Bukkit.getPlayer(request.getRequester());
        if (requester == null || !requester.isOnline()) {
            player.sendMessage("§c对方已离线");
            plugin.getTpaManager().removeRequest(request.getRequester());
            Bukkit.getScheduler().cancelTask(request.getTaskId());
            return;
        }
        Bukkit.getScheduler().cancelTask(request.getTaskId());
        plugin.getTpaManager().removeRequest(request.getRequester());

        if (request.getType() == TpaManager.TpaType.TPAH) {
            player.teleportAsync(requester.getLocation());
            player.sendMessage("§e[Funstart] §a已传送到 §b" + requester.getName() + " §a身边");
            requester.sendMessage("§e[Funstart] §b" + player.getName() + " §a已接受邀请, 传送至你身边");
        } else {
            requester.teleportAsync(player.getLocation());
            player.sendMessage("§e[Funstart] §a已同意传送请求");
            requester.sendMessage("§e[Funstart] §a已传送至 §b" + player.getName() + " §a身边");
        }
    }

    private void handleReject(Player player) {
        TpaManager.TpaRequest request = plugin.getTpaManager().getRequestByTarget(player.getUniqueId());
        if (request == null) {
            player.sendMessage("§c没有待处理的传送请求");
            return;
        }
        Player requester = Bukkit.getPlayer(request.getRequester());
        plugin.getTpaManager().removeRequest(request.getRequester());
        Bukkit.getScheduler().cancelTask(request.getTaskId());
        player.sendMessage("§e[Funstart] §c已拒绝");
        if (requester != null && requester.isOnline()) {
            requester.sendMessage("§e[Funstart] §b" + player.getName() + " §c已拒绝");
        }
    }

    private void handleBackdie(Player player) {
        BackTeleportUtil.backToDeath(player, plugin);
        player.closeInventory();
    }

    private void handleBackhome(Player player) {
        BackTeleportUtil.backToHome(player, plugin);
        player.closeInventory();
    }

    private void handleBackworld(Player player) {
        BackTeleportUtil.backToWorld(player, plugin);
        player.closeInventory();
    }

    // ---- Holder ----

    public static class TpagHolder implements InventoryHolder {
        enum View { MAIN, PENDING }
        private final Player player;
        private final boolean isHere;
        private final View view;

        public TpagHolder(Player player, boolean isHere, View view) {
            this.player = player;
            this.isHere = isHere;
            this.view = view;
        }

        public Player getPlayer() { return player; }
        public boolean isHere() { return isHere; }
        public View getView() { return view; }

        @Override
        public Inventory getInventory() { return null; }
    }

    // ---- Helpers ----

    private static ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}
