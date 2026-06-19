package moe.hinakusoft.funstart.listener;

import java.util.List;
import java.util.UUID;
import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.PlayerData;
import moe.hinakusoft.funstart.model.WarpPoint;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

public class AdminGuiListener implements Listener {

    private final FunstartPlugin plugin;

    public AdminGuiListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    public static void openMain(Player player, FunstartPlugin plugin) {
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, AdminHolder.View.MAIN), 27, "§4§l管理面板");

        inv.setItem(10, makeItem(Material.REDSTONE_BLOCK, "§c§l踢出玩家",
            "§7将玩家踢出服务器"));
        inv.setItem(11, makeItem(Material.COMMAND_BLOCK, "§6§l游戏模式",
            "§7切换自己的游戏模式"));
        inv.setItem(12, makeItem(Material.ENDER_PEARL, "§a§l免费传送点",
            "§7无点数消耗传送至传送点"));
        inv.setItem(13, makeItem(Material.ENDER_EYE, "§b§lTPA (直接传送)",
            "§7无需对方同意传送到对方"));
        inv.setItem(14, makeItem(Material.CHORUS_FRUIT, "§d§lTPAH (拉取)",
            "§7无需对方同意拉取对方"));
        inv.setItem(15, makeItem(Material.ENDER_CHEST, "§7§l无声 TPA",
            "§7无提示传送到对方"));
        inv.setItem(16, makeItem(Material.SHULKER_BOX, "§7§l无声 TPAH",
            "§7无提示拉取对方"));

        inv.setItem(17, makeItem(Material.GOLDEN_AXE, "§6§l领地管理",
            "§7管理所有领地",
            "§e传送到领地中心",
            "§e重新圈地",
            "§e删除领地"));

        // Row 3: Data & NBT editing (OP only)
        inv.setItem(18, makeItem(Material.COMMAND_BLOCK, "§4§l数据修改",
            "§c§l⚠ 该功能具有危险性!",
            "§7修改物品数据 (不可破坏/隐藏标签)",
            "§e点击后手持物品输入 §ff §e继续"));

        inv.setItem(19, makeItem(Material.NAME_TAG, "§b§lNBT 修改",
            "§7修改物品 NBT 标签",
            "§e点击后手持物品输入 §ef §e继续"));

        inv.setItem(22, makeItem(Material.ENCHANTED_BOOK, "§c§l重置密码",
            "§7重置玩家账号密码",
            "§e选择玩家后输入新密码"));

        inv.setItem(26, makeItem(Material.BARRIER, "§c关闭"));

        player.openInventory(inv);
    }

    // ---- Kick ----

    private static void openKickSelect(Player player, FunstartPlugin plugin) {
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, AdminHolder.View.KICK_SELECT), 54, "§c选择要踢出的玩家");
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

    private static void openKickConfirm(Player player, FunstartPlugin plugin, UUID targetUuid, String targetName) {
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, AdminHolder.View.KICK_CONFIRM, targetUuid, targetName), 27, "§c确认踢出");
        inv.setItem(11, makeItem(Material.GREEN_CONCRETE, "§a§l确认踢出 §b" + targetName));
        inv.setItem(15, makeItem(Material.RED_CONCRETE, "§c§l取消"));
        inv.setItem(22, makeItem(Material.BARRIER, "§c返回"));
        player.openInventory(inv);
    }

    // ---- GameMode ----

    private static void openGameModeSelect(Player player, FunstartPlugin plugin) {
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, AdminHolder.View.GAMEMODE), 27, "§6选择游戏模式");
        inv.setItem(11, makeItem(Material.GRASS_BLOCK, "§a§l生存模式", "§7切换为生存模式"));
        inv.setItem(12, makeItem(Material.GOLDEN_PICKAXE, "§6§l创造模式", "§7切换为创造模式"));
        inv.setItem(13, makeItem(Material.ENDER_EYE, "§d§l旁观模式", "§7切换为旁观模式"));
        inv.setItem(14, makeItem(Material.IRON_SWORD, "§b§l冒险模式", "§7切换为冒险模式"));
        inv.setItem(26, makeItem(Material.BARRIER, "§c返回"));
        player.openInventory(inv);
    }

    // ---- Free Warp TP ----

    private static void openFreeWarpList(Player player, FunstartPlugin plugin) {
        List<WarpPoint> warps = plugin.getWarpManager().getAvailableWarps(player);
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, AdminHolder.View.FREE_WARP), 54, "§a免费传送点");
        int slot = 0;
        for (WarpPoint w : warps) {
            if (slot >= 45) break;
            boolean isShared = !w.getOwner().equals(player.getUniqueId());
            ItemStack item = new ItemStack(isShared ? Material.MAP : Material.COMPASS);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§b" + w.getName());
            meta.setLore(List.of("§7世界: " + w.getWorldName(), "§e点击免费传送"));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        inv.setItem(53, makeItem(Material.BARRIER, "§c返回"));
        player.openInventory(inv);
    }

    // ---- Player select for TPA/TPAH actions ----

    private static void openPlayerSelect(Player player, FunstartPlugin plugin, AdminHolder.View view) {
        String title = switch (view) {
            case TPA -> "§b选择传送目标 (直接)";
            case TPAH -> "§d选择拉取目标 (强制)";
            case TPA_SILENT -> "§7选择传送目标 (无声)";
            case TPAH_SILENT -> "§7选择拉取目标 (无声)";
            default -> "§8选择玩家";
        };
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, view), 54, title);
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

    // ---- Password Reset ----

    private static void openPasswordResetSelect(Player player, FunstartPlugin plugin) {
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, AdminHolder.View.PASSWORD_RESET), 54, "§c选择要重置密码的玩家");
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

    // ---- Holder ----

    public static class AdminHolder implements InventoryHolder {
        enum View { MAIN, KICK_SELECT, KICK_CONFIRM, GAMEMODE, FREE_WARP, TPA, TPAH, TPA_SILENT, TPAH_SILENT, CLAIM_LIST, CLAIM_DETAIL, PASSWORD_RESET }

        private final Player player;
        private final View view;
        private final UUID targetUuid;
        private final String targetName;

        public AdminHolder(Player player, View view) {
            this(player, view, null, null);
        }

        public AdminHolder(Player player, View view, UUID targetUuid, String targetName) {
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
        if (!(event.getInventory().getHolder() instanceof AdminHolder holder)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;
        if (!player.isOp()) { player.closeInventory(); return; }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.getView()) {
            case MAIN -> handleMainClick(player, slot);
            case KICK_SELECT -> handleKickSelectClick(player, slot, event);
            case KICK_CONFIRM -> handleKickConfirmClick(player, slot, holder);
            case GAMEMODE -> handleGameModeClick(player, slot, holder);
            case FREE_WARP -> handleFreeWarpClick(player, slot, holder);
            case TPA, TPAH, TPA_SILENT, TPAH_SILENT -> handleTeleportClick(player, slot, holder.getView(), event);
            case CLAIM_LIST -> handleClaimListClick(player, slot, holder);
            case CLAIM_DETAIL -> handleClaimDetailClick(player, slot, holder);
            case PASSWORD_RESET -> handlePasswordResetClick(player, slot, event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AdminHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {}

    // ---- Click handlers ----

    private void handleMainClick(Player player, int slot) {
        switch (slot) {
            case 10 -> openKickSelect(player, plugin);
            case 11 -> openGameModeSelect(player, plugin);
            case 12 -> openFreeWarpList(player, plugin);
            case 13 -> openPlayerSelect(player, plugin, AdminHolder.View.TPA);
            case 14 -> openPlayerSelect(player, plugin, AdminHolder.View.TPAH);
            case 15 -> openPlayerSelect(player, plugin, AdminHolder.View.TPA_SILENT);
            case 16 -> openPlayerSelect(player, plugin, AdminHolder.View.TPAH_SILENT);
            case 17 -> openClaimList(player, plugin);
            case 18 -> {
                player.closeInventory();
                plugin.addPendingChatAction(player.getUniqueId(),
                    FunstartPlugin.PendingChatAction.Type.DATA_EDIT_CONFIRM, null, 600L);
                player.sendMessage("§4§l⚠ 警告: 该功能具有危险性, 错误操作可能导致物品损坏!");
                player.sendMessage("§4[数据修改] §7请手持要修改的物品, 输入 §ef §7继续");
            }
            case 19 -> {
                player.closeInventory();
                plugin.addPendingChatAction(player.getUniqueId(),
                    FunstartPlugin.PendingChatAction.Type.NBT_EDIT_CONFIRM, null, 600L);
                player.sendMessage("§b[NBT修改] §7请手持要修改的物品, 输入 §ef §7继续");
            }
            case 22 -> openPasswordResetSelect(player, plugin);
            case 26 -> player.closeInventory();
        }
    }

    private void handleKickSelectClick(Player player, int slot, InventoryClickEvent event) {
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
            openKickSelect(player, plugin);
            return;
        }
        openKickConfirm(player, plugin, target.getUniqueId(), target.getName());
    }

    private void handleKickConfirmClick(Player player, int slot, AdminHolder holder) {
        if (slot == 11) {
            UUID tuid = holder.getTargetUuid();
            String tname = holder.getTargetName();
            if (tuid == null || tname == null) return;
            Player target = Bukkit.getPlayer(tuid);
            if (target != null && target.isOnline()) {
                target.kickPlayer("§c你已被管理员踢出");
                plugin.getLogger().info("[Admin] " + player.getName() + " 踢出了 " + tname);
            }
            player.closeInventory();
        } else if (slot == 15 || slot == 22) {
            openMain(player, plugin);
        }
    }

    private void handleGameModeClick(Player player, int slot, AdminHolder holder) {
        GameMode gm = switch (slot) {
            case 11 -> GameMode.SURVIVAL;
            case 12 -> GameMode.CREATIVE;
            case 13 -> GameMode.SPECTATOR;
            case 14 -> GameMode.ADVENTURE;
            default -> null;
        };
        if (gm != null) {
            player.setGameMode(gm);
            player.sendMessage("§e[管理] §a已切换游戏模式为 §b" + gm.name());
            plugin.getLogger().info("[Admin] " + player.getName() + " 切换模式为 " + gm.name());
            openMain(player, plugin);
            return;
        }
        if (slot == 26) openMain(player, plugin);
    }

    private void handleFreeWarpClick(Player player, int slot, AdminHolder holder) {
        if (slot >= 45 && slot == 53) {
            openMain(player, plugin);
            return;
        }
        List<WarpPoint> warps = plugin.getWarpManager().getAvailableWarps(player);
        if (slot < 0 || slot >= warps.size()) return;
        WarpPoint w = warps.get(slot);
        World world = Bukkit.getWorld(w.getWorldName());
        if (world == null) {
            player.sendMessage("§c传送点所在世界不可用");
            return;
        }
        Location loc = new Location(world, w.getX(), w.getY(), w.getZ(), w.getYaw(), w.getPitch());
        player.teleportAsync(loc);
        player.sendMessage("§e[管理] §a已免费传送至 §b" + w.getName());
        plugin.getLogger().info("[Admin] " + player.getName() + " 免费传送至 " + w.getName());
        player.closeInventory();
    }

    private void handleTeleportClick(Player player, int slot, AdminHolder.View view, InventoryClickEvent event) {
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
            return;
        }

        boolean silent = (view == AdminHolder.View.TPA_SILENT || view == AdminHolder.View.TPAH_SILENT);
        switch (view) {
            case TPA, TPA_SILENT -> {
                player.teleportAsync(target.getLocation());
                player.sendMessage("§e[管理] §a已传送至 §b" + target.getName());
                if (!silent) target.sendMessage("§e[管理] §b" + player.getName() + " §a传送到了你身边");
                plugin.getLogger().info("[Admin] " + player.getName() + " TPA至 " + target.getName() + (silent ? " (无声)" : ""));
            }
            case TPAH, TPAH_SILENT -> {
                target.teleportAsync(player.getLocation());
                player.sendMessage("§e[管理] §a已将 §b" + target.getName() + " §a拉取至身边");
                if (!silent) target.sendMessage("§e[管理] §b" + player.getName() + " §a将你拉取到了身边");
                plugin.getLogger().info("[Admin] " + player.getName() + " TPAH拉取了 " + target.getName() + (silent ? " (无声)" : ""));
            }
        }
        player.closeInventory();
    }

    // ---- Claim Management ----

    private static void openClaimList(Player player, FunstartPlugin plugin) {
        List<moe.hinakusoft.funstart.model.ClaimRegion> claims = plugin.getClaimManager().getAllClaims();
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, AdminHolder.View.CLAIM_LIST), 54, "§6§l领地列表");
        int slot = 0;
        for (moe.hinakusoft.funstart.model.ClaimRegion c : claims) {
            if (slot >= 45) break;
            String ownerName = c.getOwner() != null ? Bukkit.getOfflinePlayer(c.getOwner()).getName() : "未知";
            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§b" + ownerName + " §7的领地");
            int xc = (c.getX1() + c.getX2()) / 2;
            int zc = (c.getZ1() + c.getZ2()) / 2;
            meta.setLore(List.of(
                "§7世界: " + c.getWorldName(),
                "§7坐标: §e" + xc + ", " + zc,
                "§7体积: §e" + c.getVolume() + " §7方块",
                "§7信任玩家: §e" + c.getTrustedPlayers().size() + " §7人",
                "§a点击管理"
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        inv.setItem(49, makeItem(Material.BARRIER, "§c返回"));
        player.openInventory(inv);
    }

    private static void openClaimDetail(Player player, FunstartPlugin plugin, moe.hinakusoft.funstart.model.ClaimRegion claim) {
        Inventory inv = Bukkit.createInventory(new AdminHolder(player, AdminHolder.View.CLAIM_DETAIL, claim.getOwner(), null), 27, "§6§l领地管理");

        String ownerName = claim.getOwner() != null ? Bukkit.getOfflinePlayer(claim.getOwner()).getName() : "未知";
        int xc = (claim.getX1() + claim.getX2()) / 2;
        int yc = (claim.getY1() + claim.getY2()) / 2;
        int zc = (claim.getZ1() + claim.getZ2()) / 2;

        inv.setItem(4, makeItem(Material.GRASS_BLOCK, "§b" + ownerName + " §7的领地",
            "§7世界: " + claim.getWorldName(),
            "§7范围: §e(" + claim.getX1() + ", " + claim.getY1() + ", " + claim.getZ1() + ") §7→ §e(" + claim.getX2() + ", " + claim.getY2() + ", " + claim.getZ2() + ")",
            "§7中心: §e" + xc + ", " + yc + ", " + zc,
            "§7体积: §e" + claim.getVolume() + " §7方块",
            "§7信任玩家: §e" + claim.getTrustedPlayers().size() + " §7人"));

        inv.setItem(11, makeItem(Material.ENDER_PEARL, "§a§l传送至领地中心",
            "§7传送至 §e" + xc + ", " + yc + ", " + zc));

        inv.setItem(13, makeItem(Material.GOLDEN_AXE, "§6§l重新圈地",
            "§7删除旧领地并创建新的",
            "§7新领地将分配给原玩家"));

        inv.setItem(15, makeItem(Material.REDSTONE_BLOCK, "§c§l删除领地",
            "§7删除该领地"));

        inv.setItem(22, makeItem(Material.BARRIER, "§c返回"));

        player.openInventory(inv);
    }

    private void handleClaimListClick(Player player, int slot, AdminHolder holder) {
        if (slot == 49) {
            openMain(player, plugin);
            return;
        }
        List<moe.hinakusoft.funstart.model.ClaimRegion> claims = plugin.getClaimManager().getAllClaims();
        if (slot < 0 || slot >= claims.size()) return;
        moe.hinakusoft.funstart.model.ClaimRegion claim = claims.get(slot);
        openClaimDetail(player, plugin, claim);
    }

    private void handleClaimDetailClick(Player player, int slot, AdminHolder holder) {
        UUID ownerUuid = holder.getTargetUuid();
        if (ownerUuid == null) { openMain(player, plugin); return; }
        moe.hinakusoft.funstart.model.ClaimRegion claim = plugin.getClaimManager().getClaimByOwner(ownerUuid);
        if (claim == null) {
            player.sendMessage("§c该领地已不存在");
            openMain(player, plugin);
            return;
        }

        if (slot == 11) {
            // Teleport to claim center
            World world = Bukkit.getWorld(claim.getWorldName());
            if (world == null) {
                player.sendMessage("§c领地所在世界不可用");
                return;
            }
            int xc = (claim.getX1() + claim.getX2()) / 2;
            int yc = (claim.getY1() + claim.getY2()) / 2;
            int zc = (claim.getZ1() + claim.getZ2()) / 2;
            player.teleportAsync(new Location(world, xc + 0.5, yc + 0.5, zc + 0.5));
            player.sendMessage("§e[管理] §a已传送至领地中心");
            plugin.getLogger().info("[Admin] " + player.getName() + " 传送至 " + ownerUuid + " 的领地中心");
            player.closeInventory();
        } else if (slot == 13) {
            // Re-claim: delete old claim and start a new claim session for admin
            plugin.getClaimManager().deleteClaim(ownerUuid);
            plugin.getClaimManager().setReclaimTarget(player.getUniqueId(), ownerUuid);
            plugin.getClaimManager().startClaimSession(player);
            player.sendMessage("§6[管理] §a已删除旧领地, 请重新选择圈地范围");
            player.sendMessage("§7新领地创建后将分配给原玩家");
        } else if (slot == 15) {
            // Delete claim
            plugin.getClaimManager().deleteClaim(ownerUuid);
            player.sendMessage("§e[管理] §a已删除该领地");
            plugin.getLogger().info("[Admin] " + player.getName() + " 删除了 " + ownerUuid + " 的领地");
            openMain(player, plugin);
        } else if (slot == 22) {
            openClaimList(player, plugin);
        }
    }

    private void handlePasswordResetClick(Player player, int slot, InventoryClickEvent event) {
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
            openPasswordResetSelect(player, plugin);
            return;
        }
        var authManager = plugin.getAuthManager();
        if (authManager == null || !authManager.isRegistered(target.getUniqueId())) {
            player.sendMessage("§c该玩家尚未注册账号");
            openPasswordResetSelect(player, plugin);
            return;
        }
        player.closeInventory();
        plugin.addPendingChatAction(player.getUniqueId(),
            FunstartPlugin.PendingChatAction.Type.ADMIN_RESET_PASSWORD,
            target.getUniqueId(), 600L);
        player.sendMessage("§c[管理] §e请输入 " + targetName + " §e的新密码 (至少4位):");
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
