package moe.hinakusoft.funstart.listener;

import java.util.List;
import java.util.UUID;
import moe.hinakusoft.funstart.FunstartPlugin;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class GuiListener implements Listener {

    private final FunstartPlugin plugin;

    public GuiListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    public static void openFor(Player player, FunstartPlugin plugin) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        Inventory inv = Bukkit.createInventory(new FstgHolder(player), 27, "§8⚙ Funstart 设置");

        inv.setItem(11, makeToggleItem(
            Material.DIAMOND_PICKAXE,
            "§6§l每日任务",
            List.of(
                "§7查看并完成每日任务",
                "§7获得额外点数奖励",
                "§e点击打开每日任务"
            )
        ));

        inv.setItem(13, makeToggleItem(
            data.isChainEnabled() ? Material.CHAIN : Material.BARRIER,
            "§7§l连锁挖掘",
            List.of(
                "§7潜行时连锁破坏同类方块",
                "§7每16个非矿物方块消耗1点",
                "§a状态: " + (data.isChainEnabled() ? "§a已开启" : "§c已关闭")
            )
        ));

        inv.setItem(14, makeToggleItem(
            data.isAutoFix() ? Material.SMITHING_TABLE : Material.BARRIER,
            "§b§l自动修复",
            List.of(
                "§7自动修复耐久低于20%的物品",
                "§7每50耐久消耗1点 (每5秒检查)",
                "§a状态: " + (data.isAutoFix() ? "§a已开启" : "§c已关闭"),
                "§e点击开关自动修复"
            )
        ));

        inv.setItem(15, makeToggleItem(
            data.isHarvestEnabled() ? Material.WHEAT : Material.BARRIER,
            "§e§l范围收割",
            List.of(
                "§7潜行+锄头收割成熟作物",
                "§7自动重新播种，每4个扣1点",
                "§a状态: " + (data.isHarvestEnabled() ? "§a已开启" : "§c已关闭")
            )
        ));

        inv.setItem(20, makeToggleItem(
            Material.GOLD_INGOT,
            "§6§l转账点数",
            List.of(
                "§7将点数转账给其他玩家",
                "§7手续费: 转账金额的2%",
                "§e点击选择转账目标"
            )
        ));

        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean canFix = tool != null && !tool.getType().isAir()
            && tool.getItemMeta() instanceof Damageable
            && tool.getType().getMaxDurability() > 0;
        int currentDurability = 0;
        int maxDurability = 0;
        if (canFix) {
            maxDurability = tool.getType().getMaxDurability();
            currentDurability = maxDurability - ((Damageable) tool.getItemMeta()).getDamage();
        }
        boolean needsFix = canFix && (double) currentDurability / maxDurability < 1.0;
        double fixCost = needsFix ? (double) (maxDurability - currentDurability) / 50.0 : 0.0;

        inv.setItem(22, makeToggleItem(
            needsFix ? Material.ANVIL : Material.BARRIER,
            "§5§l耐久修复 (手持)",
            List.of(
                "§7修复手持工具至满耐久",
                needsFix ? "§7当前耐久: §e" + currentDurability + "§7/§e" + maxDurability : "§7手持工具已是满耐久",
                needsFix ? "§7修复消耗: §e" + PlayerData.fmt(fixCost) + " §7点" : "",
                "§e点击修复工具 (关闭界面)"
            )
        ));

        ItemStack allFix = new ItemStack(Material.ANVIL);
        allFix.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
        ItemMeta allFixMeta = allFix.getItemMeta();
        allFixMeta.setDisplayName("§d§l全部修复 (全身)");
        int repairableCount = 0;
        double allCost = 0.0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (!(item.getItemMeta() instanceof Damageable d)) continue;
            int md = item.getType().getMaxDurability();
            if (md <= 0) continue;
            int cd = md - d.getDamage();
            if ((double) cd / md < 1.0) {
                repairableCount++;
                allCost += (double) (md - cd) / 50.0;
            }
        }
        allFixMeta.setLore(List.of(
            "§7修复全身所有非满耐久的物品",
            repairableCount > 0 ? "§7可修复: §e" + repairableCount + " §7个物品, 共 §e" + PlayerData.fmt(allCost) + " §7点" : "§7没有需要修复的物品",
            "§e点击后输入 1 确认, 0 取消"
        ));
        allFix.setItemMeta(allFixMeta);
        inv.setItem(23, allFix);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setPlayerProfile(player.getPlayerProfile());
        headMeta.setDisplayName("§b" + player.getName());
        headMeta.setLore(List.of(
            "§7❤ 生命: §c" + String.format("%.0f", player.getHealth()) + "§7/§c" + String.format("%.0f", player.getMaxHealth()),
            "§7🍖 饱食度: §e" + player.getFoodLevel(),
            "§7💎 点数: §b" + PlayerData.fmt(data.getPoints())
        ));
        head.setItemMeta(headMeta);
        inv.setItem(26, head);

        player.openInventory(inv);
    }

    private static ItemStack makeToggleItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // --------- Player selection GUI for bank transfer ---------

    public static void openPlayerSelect(Player player, FunstartPlugin plugin) {
        Inventory inv = Bukkit.createInventory(new PlayerSelectHolder(player), 54, "§6选择转账目标");
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;
            if (slot >= 45) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setPlayerProfile(target.getPlayerProfile());
            meta.setDisplayName("§b" + target.getName());
            meta.setLore(List.of("§e点击选择此玩家"));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }
        if (slot == 0) {
            player.sendMessage("§c没有其他在线玩家");
            return;
        }
        player.openInventory(inv);
    }

    public static class PlayerSelectHolder implements InventoryHolder {
        private final Player player;
        public PlayerSelectHolder(Player player) { this.player = player; }
        public Player getPlayer() { return player; }
        @Override public Inventory getInventory() { return null; }
    }

    // --------- Bank transfer confirmation GUI ---------

    public static class BankConfirmHolder implements InventoryHolder {
        private final Player player;
        public final UUID targetUuid;
        public final String targetName;
        public final double amount;

        public BankConfirmHolder(Player player, UUID targetUuid, String targetName, double amount) {
            this.player = player;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.amount = amount;
        }
        public Player getPlayer() { return player; }
        @Override public Inventory getInventory() { return null; }
    }

    public static void openBankConfirm(Player player, FunstartPlugin plugin, UUID targetUuid, String targetName, double amount) {
        Inventory inv = Bukkit.createInventory(new BankConfirmHolder(player, targetUuid, targetName, amount), 27, "§6转账确认");
        double tax = Math.max(amount * 0.02, 1.0);
        double total = amount + tax;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6转账明细");
        infoMeta.setLore(List.of(
            "§7目标: §b" + targetName,
            "§7金额: §e" + PlayerData.fmt(amount) + " §7点",
            "§7税费: §c" + PlayerData.fmt(tax) + " §7点 (2%)",
            "§7总计: §e" + PlayerData.fmt(total) + " §7点",
            "§7当前余额: §b" + PlayerData.fmt(data.getPoints()) + " §7点",
            data.getPoints() >= total ? "§a余额充足" : "§c余额不足"
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(13, info);

        // Confirm button
        ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§l确认转账");
        confirm.setItemMeta(confirmMeta);
        inv.setItem(11, confirm);

        // Modify button
        ItemStack modify = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta modifyMeta = modify.getItemMeta();
        modifyMeta.setDisplayName("§e§l修改金额");
        modify.setItemMeta(modifyMeta);
        inv.setItem(15, modify);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§l取消");
        cancel.setItemMeta(cancelMeta);
        inv.setItem(22, cancel);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();

        if (holder instanceof FstgHolder fstgHolder) {
            handleFstgClick(event, fstgHolder);
        } else if (holder instanceof PlayerSelectHolder selectHolder) {
            handlePlayerSelectClick(event, selectHolder);
        } else if (holder instanceof BankConfirmHolder bankHolder) {
            handleBankConfirmClick(event, bankHolder);
        }
    }

    private void handleFstgClick(InventoryClickEvent event, FstgHolder holder) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        if (slot == 11) {
            player.closeInventory();
            DailyTaskListener.openFor(player, plugin);
            return;
        }
        if (slot == 13) {
            boolean newState = !data.isChainEnabled();
            data.setChainEnabled(newState);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
            event.getInventory().setItem(13, makeToggleItem(
                newState ? Material.CHAIN : Material.BARRIER,
                "§7§l连锁挖掘",
                List.of(
                    "§7潜行时连锁破坏同类方块",
                    "§7每16个非矿物方块消耗1点",
                    "§a状态: " + (newState ? "§a已开启" : "§c已关闭")
                )
            ));
            player.sendMessage("§e[Funstart] " + (newState ? "§a已开启连锁挖掘" : "§c已关闭连锁挖掘"));
            return;
        }
        if (slot == 14) {
            boolean newState = !data.isAutoFix();
            data.setAutoFix(newState);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
            event.getInventory().setItem(14, makeToggleItem(
                newState ? Material.SMITHING_TABLE : Material.BARRIER,
                "§b§l自动修复",
                List.of(
                    "§7自动修复耐久低于20%的物品",
                    "§7每50耐久消耗1点 (每5秒检查)",
                    "§a状态: " + (newState ? "§a已开启" : "§c已关闭"),
                    "§e点击开关自动修复"
                )
            ));
            player.sendMessage("§e[Funstart] " + (newState ? "§a已开启自动修复 (耐久<20%)" : "§c已关闭自动修复"));
            return;
        }
        if (slot == 15) {
            boolean newState = !data.isHarvestEnabled();
            data.setHarvestEnabled(newState);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
            event.getInventory().setItem(15, makeToggleItem(
                newState ? Material.WHEAT : Material.BARRIER,
                "§e§l范围收割",
                List.of(
                    "§7潜行+锄头收割成熟作物",
                    "§7自动重新播种，每4个扣1点",
                    "§a状态: " + (newState ? "§a已开启" : "§c已关闭")
                )
            ));
            player.sendMessage("§e[Funstart] " + (newState ? "§a已开启范围收割" : "§c已关闭范围收割"));
            return;
        }
        if (slot == 22) {
            fixAction(player, data);
            player.closeInventory();
            return;
        }
        if (slot == 20) {
            player.closeInventory();
            openPlayerSelect(player, plugin);
            return;
        }
        if (slot == 23) {
            player.closeInventory();
            plugin.addPendingChatAction(player.getUniqueId(),
                FunstartPlugin.PendingChatAction.Type.ALL_FIX_CONFIRM, "");
            double previewCost = 0.0;
            int previewCount = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) continue;
                if (!(item.getItemMeta() instanceof Damageable d)) continue;
                int md = item.getType().getMaxDurability();
                if (md <= 0) continue;
                int cd = md - d.getDamage();
            if ((double) cd / md < 1.0) {
                    previewCount++;
                    previewCost += (double) (md - cd) / 50.0;
                }
            }
            if (previewCount == 0) {
                player.sendMessage("§c没有需要修复的物品");
                return;
            }
            player.sendMessage("§6即将修复 §e" + previewCount + " §6个物品, 共消耗 §e" + PlayerData.fmt(previewCost) + " §6点");
            player.sendMessage("§a输入 §e1 §a确认, §c0 §a取消");
            return;
        }
    }

    private void handlePlayerSelectClick(InventoryClickEvent event, PlayerSelectHolder holder) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;
        if (!(item.getItemMeta() instanceof SkullMeta skullMeta)) return;
        String targetName = skullMeta.getDisplayName();
        if (targetName == null || targetName.isEmpty()) return;
        // Strip color codes to get actual name
        targetName = targetName.replace("§b", "").replace("§r", "").trim();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c该玩家已离线");
            player.closeInventory();
            return;
        }
        player.closeInventory();
        plugin.addPendingChatAction(player.getUniqueId(),
            FunstartPlugin.PendingChatAction.Type.BANK_AMOUNT, target.getUniqueId().toString());
        player.sendMessage("§a请输入转账金额 (点数)");
        player.sendMessage("§7手续费为转账金额的2% (最低1点)");
    }

    private void handleBankConfirmClick(InventoryClickEvent event, BankConfirmHolder holder) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;
        int slot = event.getRawSlot();

        if (slot == 11) {
            // Confirm
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
            Player target = Bukkit.getPlayer(holder.targetUuid);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c目标玩家已离线");
                player.closeInventory();
                return;
            }
            double tax = Math.max(holder.amount * 0.02, 1.0);
            double total = holder.amount + tax;
            if (data.getPoints() < total) {
                player.sendMessage("§c点数不足! 需要 §e" + PlayerData.fmt(total) + " §c点 (含税费)");
                player.closeInventory();
                return;
            }
            data.deductPoints(total);
            PlayerData targetData = plugin.getPlayerDataManager().getPlayerData(target);
            targetData.addPoints(holder.amount);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
            plugin.getPlayerDataManager().savePlayerData(target.getUniqueId());
            player.sendMessage("§e[Funstart] §a成功向 §b" + holder.targetName + " §a转账 §e" + PlayerData.fmt(holder.amount) + " §a点 (税费: §e" + PlayerData.fmt(tax) + "§a), 剩余 §e" + PlayerData.fmt(data.getPoints()) + " §a点");
            target.sendMessage("§e[Funstart] §b" + player.getName() + " §a向你转账了 §e" + PlayerData.fmt(holder.amount) + " §a点");
            player.closeInventory();
        } else if (slot == 15) {
            // Modify - go back to amount input
            player.closeInventory();
            plugin.addPendingChatAction(player.getUniqueId(),
                FunstartPlugin.PendingChatAction.Type.BANK_AMOUNT, holder.targetUuid.toString());
            player.sendMessage("§a请输入新的转账金额 (点数)");
            player.sendMessage("§7手续费为转账金额的2% (最低1点)");
        } else if (slot == 22) {
            // Cancel
            player.closeInventory();
            player.sendMessage("§c已取消转账");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof FstgHolder || holder instanceof PlayerSelectHolder || holder instanceof BankConfirmHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {}

    private void fixAction(Player player, PlayerData data) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType().isAir() || !(tool.getItemMeta() instanceof Damageable damageable)) {
            player.sendMessage("§c手持物品无法修复");
            return;
        }
        int maxDurability = tool.getType().getMaxDurability();
        if (maxDurability <= 0) {
            player.sendMessage("§c该物品无法修复");
            return;
        }
        int currentDurability = maxDurability - damageable.getDamage();
        if (currentDurability >= maxDurability) {
            player.sendMessage("§c工具已是满耐久, 无需修复");
            return;
        }
        int repairAmount = maxDurability - currentDurability;
        double cost = (double) repairAmount / 50.0;
        if (data.getPoints() < cost) {
            player.sendMessage("§c点数不足! 需要 §e" + PlayerData.fmt(cost) + " §c点");
            return;
        }
        data.deductPoints(cost);
        damageable.setDamage(0);
        tool.setItemMeta(damageable);
        player.sendMessage("§e[Funstart] §a已修复工具, 消耗 §e" + PlayerData.fmt(cost) + " §a点, 剩余 §e" + PlayerData.fmt(data.getPoints()) + " §a点");
    }

    public static class FstgHolder implements InventoryHolder {
        private final Player player;
        public FstgHolder(Player player) { this.player = player; }
        public Player getPlayer() { return player; }
        @Override public Inventory getInventory() { return null; }
    }
}
