package moe.hinakusoft.funstart.listener;

import java.util.ArrayList;
import java.util.List;
import moe.hinakusoft.funstart.FunstartPlugin;
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
import org.bukkit.inventory.ItemFlag;

public class DataEditGuiListener implements Listener {

    private final FunstartPlugin plugin;

    public DataEditGuiListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    private static final int ITEMS_PER_PAGE = 10;

    public static void openDataEditor(Player player, FunstartPlugin plugin) {
        openDataEditor(player, plugin, 0);
    }

    public static void openDataEditor(Player player, FunstartPlugin plugin, int page) {
        Inventory inv = Bukkit.createInventory(new DataEditHolder(player, page), 27, "§4§l数据修改");

        ItemStack held = player.getInventory().getItemInMainHand();
        inv.setItem(13, (held != null && !held.getType().isAir()) ? held.clone() : makeItem(Material.BARRIER, "§c请手持物品后重试"));

        List<DataOption> options = getOptionsForPage(page);

        // Row 2-3: data toggles, starting at slot 10
        int slot = 10;
        for (DataOption opt : options) {
            if (slot == 13) slot++;
            if (slot >= 25) break;
            boolean enabled = opt.checker.check(player);
            inv.setItem(slot++, makeToggle(opt.name, opt.flagKey, enabled));
        }

        // Fill rest of editable area with glass
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§7数据修改");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane.clone());
        }

        // Navigation
        inv.setItem(0, page > 0 ? makeItem(Material.ARROW, "§e上一页") : makeItem(Material.GRAY_STAINED_GLASS_PANE, ""));
        inv.setItem(4, makeItem(Material.BOOK, "§7第 §e" + (page + 1) + " §7页 / §e" + totalPages() + " §7页",
            "§7数据修改"));
        inv.setItem(8, page + 1 < totalPages() ? makeItem(Material.ARROW, "§e下一页") : makeItem(Material.GRAY_STAINED_GLASS_PANE, ""));

        inv.setItem(26, makeItem(Material.BARRIER, "§c关闭"));

        player.openInventory(inv);
    }

    private static int totalPages() {
        return 1; // currently 1 page (unbreakable + ItemFlags); expandable
    }

    // ---- Data Options ----

    private static List<DataOption> getOptionsForPage(int page) {
        List<DataOption> list = new ArrayList<>();
        list.add(new DataOption("§6不可破坏", "UNBREAKABLE",
            p -> p.getInventory().getItemInMainHand().hasItemMeta()
                && p.getInventory().getItemInMainHand().getItemMeta().isUnbreakable()));
        list.add(new DataOption("§d隐藏附魔", "HIDE_ENCHANTS", flagChecker(ItemFlag.HIDE_ENCHANTS)));
        list.add(new DataOption("§d隐藏属性", "HIDE_ATTRIBUTES", flagChecker(ItemFlag.HIDE_ATTRIBUTES)));
        list.add(new DataOption("§d隐藏不可破坏", "HIDE_UNBREAKABLE", flagChecker(ItemFlag.HIDE_UNBREAKABLE)));
        list.add(new DataOption("§d隐藏破坏信息", "HIDE_DESTROYS", flagChecker(ItemFlag.HIDE_DESTROYS)));
        list.add(new DataOption("§d隐藏放置信息", "HIDE_PLACED_ON", flagChecker(ItemFlag.HIDE_PLACED_ON)));
        list.add(new DataOption("§d隐藏额外信息", "HIDE_ADDITIONAL_TOOLTIP", flagChecker(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)));
        list.add(new DataOption("§d隐藏染色", "HIDE_DYE", flagChecker(ItemFlag.HIDE_DYE)));
        list.add(new DataOption("§d隐藏盔甲纹饰", "HIDE_ARMOR_TRIM", flagChecker(ItemFlag.HIDE_ARMOR_TRIM)));
        list.add(new DataOption("§d隐藏存储附魔", "HIDE_STORED_ENCHANTS", flagChecker(ItemFlag.HIDE_STORED_ENCHANTS)));
        return list;
    }

    private static DataChecker flagChecker(ItemFlag flag) {
        return p -> {
            ItemStack held = p.getInventory().getItemInMainHand();
            return held.hasItemMeta() && held.getItemMeta().hasItemFlag(flag);
        };
    }

    private record DataOption(String name, String flagKey, DataChecker checker) {}

    @FunctionalInterface
    private interface DataChecker {
        boolean check(Player player);
    }

    // ---- Apply toggle ----

    private void applyToggle(Player player, int index, DataEditHolder holder) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage("§c手持物品不能为空");
            player.closeInventory();
            return;
        }
        ItemMeta meta = held.getItemMeta();
        List<DataOption> options = getOptionsForPage(holder.getPage());
        if (index < 0 || index >= options.size()) return;
        DataOption opt = options.get(index);
        String key = opt.flagKey;

        if (key.equals("UNBREAKABLE")) {
            meta.setUnbreakable(!meta.isUnbreakable());
            plugin.getLogger().info("[Admin] " + player.getName() + " 切换不可破坏: " + meta.isUnbreakable());
        } else {
            ItemFlag flag = ItemFlag.valueOf(key);
            if (meta.hasItemFlag(flag)) {
                meta.removeItemFlags(flag);
                plugin.getLogger().info("[Admin] " + player.getName() + " 移除标签 " + key);
            } else {
                meta.addItemFlags(flag);
                plugin.getLogger().info("[Admin] " + player.getName() + " 添加标签 " + key);
            }
        }

        held.setItemMeta(meta);
        player.getInventory().setItemInMainHand(held);
        openDataEditor(player, plugin, holder.getPage());
    }

    // ---- Event handlers ----

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DataEditHolder holder)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (slot == 26) { player.closeInventory(); return; }

        // Navigation
        int page = holder.getPage();
        if (slot == 0 && page > 0) { openDataEditor(player, plugin, page - 1); return; }
        if (slot == 8 && page + 1 < totalPages()) { openDataEditor(player, plugin, page + 1); return; }

        // Map slot to option index
        int index = slotToIndex(slot, page);
        if (index >= 0) {
            applyToggle(player, index, holder);
        }
    }

    private static int slotToIndex(int slot, int page) {
        int offset;
        if (slot >= 10 && slot <= 12) offset = slot - 10;
        else if (slot >= 14 && slot <= 16) offset = slot - 11;
        else if (slot >= 19 && slot <= 24) offset = slot - 14;
        else return -1;
        return offset + page * ITEMS_PER_PAGE;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DataEditHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {}

    // ---- Holder ----

    public static class DataEditHolder implements InventoryHolder {
        private final Player player;
        private final int page;

        public DataEditHolder(Player player, int page) {
            this.player = player;
            this.page = page;
        }

        public Player getPlayer() { return player; }
        public int getPage() { return page; }

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

    private static ItemStack makeToggle(String name, String flagKey, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(enabled ? "§a✔ 已开启" : "§c✘ 已关闭");
        lore.add("§e点击切换");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
