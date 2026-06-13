package moe.hinakusoft.funstart.listener;

import java.util.List;
import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.CustomEnchantment;
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

public class EnchantDowngradeListener implements Listener {

    private final FunstartPlugin plugin;

    public EnchantDowngradeListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    public static void openFor(Player player, FunstartPlugin plugin) {
        Inventory inv = Bukkit.createInventory(new DowngradeHolder(player), 18, "§8降级自定义附魔");

        ItemStack held = player.getInventory().getItemInMainHand();
        int slot = 0;
        for (CustomEnchantment ce : CustomEnchantment.GUI_VALUES) {
            int level = held != null ? EnchantGuiListener.getCustomLevel(held, plugin, ce) : 0;
            Material mat = level > 0 ? Material.ENCHANTED_BOOK : Material.BARRIER;
            String status = level > 0 ? "§e" + EnchantGuiListener.toRoman(level) + " §a点击降级 (§e" + (level * 5) + " 点§a)" : "§7未拥有";
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§d" + ce.getDisplayName());
            meta.setLore(List.of(
                "§7" + ce.getDescription(),
                status
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // Info item
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§l降级说明");
        infoMeta.setLore(List.of("§7点击拥有的附魔进行降级", "§7等级1→移除, 更高等级→降1级", "§7降级消耗: 等级×5点", "§7不返还材料"));
        info.setItemMeta(infoMeta);
        inv.setItem(8, info);

        // Close
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c关闭");
        close.setItemMeta(closeMeta);
        inv.setItem(17, close);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DowngradeHolder holder)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 18) return;

        if (slot == 17) {
            player.closeInventory();
            return;
        }

        if (slot >= CustomEnchantment.GUI_VALUES.size()) return;

        CustomEnchantment ce = CustomEnchantment.GUI_VALUES.get(slot);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage("§c请手持要降级的物品");
            player.closeInventory();
            return;
        }

        int currentLevel = EnchantGuiListener.getCustomLevel(held, plugin, ce);
        if (currentLevel <= 0) {
            player.sendMessage("§c该物品没有§d" + ce.getDisplayName() + "§c附魔");
            return;
        }

        int cost = currentLevel * 5;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data.getPoints() < cost) {
            player.sendMessage("§c点数不足! 需要 §e" + PlayerData.fmt(cost) + " §c点");
            return;
        }

        data.deductPoints(cost);
        if (currentLevel <= 1) {
            EnchantGuiListener.setCustomLevel(held, plugin, ce, 0);
            player.sendMessage("§e[Funstart] §a已移除 §d" + ce.getDisplayName() + " §a附魔, 消耗 §e" + PlayerData.fmt(cost) + " §a点");
        } else {
            EnchantGuiListener.setCustomLevel(held, plugin, ce, currentLevel - 1);
            player.sendMessage("§e[Funstart] §a已将 §d" + ce.getDisplayName() + " §a降级至 §e" + EnchantGuiListener.toRoman(currentLevel - 1) + " §a, 消耗 §e" + PlayerData.fmt(cost) + " §a点");
        }
        openFor(player, plugin);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DowngradeHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {}

    public static class DowngradeHolder implements InventoryHolder {
        private final Player player;
        public DowngradeHolder(Player player) { this.player = player; }
        public Player getPlayer() { return player; }
        @Override public Inventory getInventory() { return null; }
    }
}
