package moe.hinakusoft.funstart.listener;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import moe.hinakusoft.funstart.FunstartPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PrankGuiListener implements Listener {

    private final FunstartPlugin plugin;
    private static final Set<UUID> invisiblePlayers = ConcurrentHashMap.newKeySet();

    public static boolean isInvisible(UUID uid) { return invisiblePlayers.contains(uid); }

    public PrankGuiListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- Tick task for invisible ActionBar ----

    public void startInvisibleTick() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uid : invisiblePlayers) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null && p.isOnline()) {
                    p.sendActionBar("§7[§8隐身中§7]");
                }
            }
        }, 0L, 20L);
    }

    public Set<UUID> getInvisiblePlayers() { return invisiblePlayers; }

    // ---- Open ----

    public static void openMain(Player player, FunstartPlugin plugin) {
        Inventory inv = Bukkit.createInventory(new PrankHolder(player), 27, "§5§l恶搞面板");

        boolean isInvisible = invisiblePlayers.contains(player.getUniqueId());

        inv.setItem(10, makeItem(
            isInvisible ? Material.GLASS : Material.TINTED_GLASS,
            (isInvisible ? "§a" : "§7") + "§l完全隐身",
            isInvisible ? "§7状态: §a已开启" : "§7状态: §c已关闭",
            "§e点击" + (isInvisible ? "关闭" : "开启")));

        inv.setItem(12, makeItem(Material.LIGHTNING_ROD, "§e§l劈个雷吧",
            "§7在原地放一道闪电"));

        inv.setItem(14, makeItem(Material.TNT, "§c§l虚假爆炸",
            "§7产生无伤害的爆炸特效"));

        inv.setItem(16, makeItem(Material.ENDER_CHEST, "§6§l移动物品栏",
            "§7查看其他玩家的背包"));

        inv.setItem(26, makeItem(Material.BARRIER, "§c关闭"));

        player.openInventory(inv);
    }

    // ---- Inventory viewer ----

    private static void openInventoryViewer(Player player, FunstartPlugin plugin) {
        Inventory inv = Bukkit.createInventory(new PrankHolder(player, PrankHolder.View.INV_SELECT), 54, "§6选择玩家查看物品栏");
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;
            if (slot >= 45) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setPlayerProfile(target.getPlayerProfile());
            meta.setDisplayName("§b" + target.getName());
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }
        inv.setItem(53, makeItem(Material.BARRIER, "§c返回"));
        player.openInventory(inv);
    }

    private static void openPlayerInventory(Player player, FunstartPlugin plugin, Player target) {
        Inventory inv = Bukkit.createInventory(new PrankHolder(player, PrankHolder.View.INV_VIEW, target.getUniqueId(), target.getName()), 54, "§6" + target.getName() + " 的物品栏");

        // Hotbar slots 0-8
        for (int i = 0; i < 9; i++) {
            ItemStack item = target.getInventory().getItem(i);
            inv.setItem(i, item != null ? item.clone() : null);
        }
        // Inventory slots 9-35
        for (int i = 9; i < 36; i++) {
            ItemStack item = target.getInventory().getItem(i);
            inv.setItem(i + 9, item != null ? item.clone() : null);
        }
        // Armor slots 36-38
        inv.setItem(36, target.getInventory().getHelmet() != null ? target.getInventory().getHelmet().clone() : null);
        inv.setItem(37, target.getInventory().getChestplate() != null ? target.getInventory().getChestplate().clone() : null);
        inv.setItem(38, target.getInventory().getLeggings() != null ? target.getInventory().getLeggings().clone() : null);
        inv.setItem(39, target.getInventory().getBoots() != null ? target.getInventory().getBoots().clone() : null);
        // Offhand slot 40
        inv.setItem(40, target.getInventory().getItemInOffHand() != null ? target.getInventory().getItemInOffHand().clone() : null);

        // Labels
        ItemStack hotbarLabel = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§7快捷栏");
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, hotbarLabel.clone());
        }
        ItemStack invLabel = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§7背包");
        for (int i = 18; i < 45; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, invLabel.clone());
        }

        inv.setItem(45, makeItem(Material.DIAMOND_HELMET, "§b头盔"));
        inv.setItem(46, makeItem(Material.DIAMOND_CHESTPLATE, "§b胸甲"));
        inv.setItem(47, makeItem(Material.DIAMOND_LEGGINGS, "§b护腿"));
        inv.setItem(48, makeItem(Material.DIAMOND_BOOTS, "§b靴子"));
        inv.setItem(49, makeItem(Material.SHIELD, "§f副手"));

        inv.setItem(53, makeItem(Material.BARRIER, "§c关闭"));

        player.openInventory(inv);
    }

    // ---- Holder ----

    public static class PrankHolder implements InventoryHolder {
        enum View { MAIN, INV_SELECT, INV_VIEW }

        private final Player player;
        private final View view;
        private final UUID targetUuid;
        private final String targetName;

        public PrankHolder(Player player) {
            this(player, View.MAIN, null, null);
        }

        public PrankHolder(Player player, View view) {
            this(player, view, null, null);
        }

        public PrankHolder(Player player, View view, UUID targetUuid, String targetName) {
            this.player = player;
            this.view = view;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
        }

        public Player getPlayer() { return player; }
        public View getView() { return view; }
        public UUID getTargetUuid() { return targetUuid; }
        public String getTargetName() { return targetName; }

        @Override public Inventory getInventory() { return null; }
    }

    // ---- Events ----

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PrankHolder holder)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;
        if (!player.isOp()) { player.closeInventory(); return; }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.getView()) {
            case MAIN -> handleMainClick(player, slot);
            case INV_SELECT -> handleInvSelectClick(player, slot, event);
            case INV_VIEW -> handleInvViewClick(player, slot, holder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PrankHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {}

    // ---- Click handlers ----

    private void handleMainClick(Player player, int slot) {
        switch (slot) {
            case 10 -> toggleInvisibility(player);
            case 12 -> strikeLightning(player);
            case 14 -> fakeExplosion(player);
            case 16 -> openInventoryViewer(player, plugin);
            case 26 -> player.closeInventory();
        }
    }

    private void handleInvSelectClick(Player player, int slot, InventoryClickEvent event) {
        if (slot >= 45 && slot == 53) {
            openMain(player, plugin);
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String displayName = item.getItemMeta().getDisplayName();
        if (displayName == null || displayName.isEmpty()) return;
        String targetName = displayName.replace("§b", "").replace("§r", "").trim();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c该玩家已离线");
            openInventoryViewer(player, plugin);
            return;
        }
        openPlayerInventory(player, plugin, target);
    }

    private void handleInvViewClick(Player player, int slot, PrankHolder holder) {
        if (slot == 53) {
            player.closeInventory();
        }
    }

    // ---- Actions ----

    private void toggleInvisibility(Player player) {
        UUID uid = player.getUniqueId();
        if (invisiblePlayers.contains(uid)) {
            invisiblePlayers.remove(uid);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            //player.sendMessage("§e[恶搞] §c已关闭隐身");
            plugin.getLogger().info("[Prank] " + player.getName() + " 关闭了隐身");
        } else {
            invisiblePlayers.add(uid);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));
            //player.sendMessage("§e[恶搞] §a已开启完全隐身");
            player.sendActionBar("§7[§8隐身中awa§7]");
            plugin.getLogger().info("[Prank] " + player.getName() + " 开启了隐身");
        }
        openMain(player, plugin);
    }

    private void strikeLightning(Player player) {
        Location loc = player.getLocation();
        player.getWorld().strikeLightning(loc);
        //player.sendMessage("§e[恶搞] §b轰隆! 一道闪电劈了下来");
        plugin.getLogger().info("[Prank] " + player.getName() + " 在自己的位置放了闪电");
        player.closeInventory();
    }

    private void fakeExplosion(Player player) {
        Location loc = player.getLocation();
        player.getWorld().createExplosion(loc, 0f, false, false);
        //player.sendMessage("§e[恶搞] §c嘭! 发生了爆炸 (无伤害)");
        plugin.getLogger().info("[Prank] " + player.getName() + " 在自己的位置放了虚假爆炸");
        player.closeInventory();
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
