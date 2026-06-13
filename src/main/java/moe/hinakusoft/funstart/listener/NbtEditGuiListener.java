package moe.hinakusoft.funstart.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import moe.hinakusoft.funstart.FunstartPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;

public class NbtEditGuiListener implements Listener {

    private final FunstartPlugin plugin;
    private static final Map<UUID, String> pendingTagKeys = new ConcurrentHashMap<>();
    private static final int TAGS_PER_PAGE = 36;

    public NbtEditGuiListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- PDC helpers ----

    private static NamespacedKey listKey(FunstartPlugin plugin) {
        return new NamespacedKey(plugin, "nbt_tag_list");
    }

    private static NamespacedKey tagKey(FunstartPlugin plugin, String tagName) {
        return new NamespacedKey(plugin, "nbt_" + tagName);
    }

    public static List<String> getTagKeys(ItemStack item, FunstartPlugin plugin) {
        if (item == null || !item.hasItemMeta()) return new ArrayList<>();
        var pdc = item.getItemMeta().getPersistentDataContainer();
        List<String> keys = pdc.get(listKey(plugin), PersistentDataType.LIST.strings());
        return keys != null ? keys : new ArrayList<>();
    }

    public static String getTagValue(ItemStack item, FunstartPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(tagKey(plugin, key), PersistentDataType.STRING);
    }

    public static void addTag(ItemStack item, FunstartPlugin plugin, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        List<String> keys = pdc.get(listKey(plugin), PersistentDataType.LIST.strings());
        if (keys == null) keys = new ArrayList<>();
        if (!keys.contains(key)) keys.add(key);
        pdc.set(listKey(plugin), PersistentDataType.LIST.strings(), keys);
        pdc.set(tagKey(plugin, key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    public static void removeTag(ItemStack item, FunstartPlugin plugin, String key) {
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        List<String> keys = pdc.get(listKey(plugin), PersistentDataType.LIST.strings());
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) {
                pdc.remove(listKey(plugin));
            } else {
                pdc.set(listKey(plugin), PersistentDataType.LIST.strings(), keys);
            }
        }
        pdc.remove(tagKey(plugin, key));
        item.setItemMeta(meta);
    }

    // ---- Open GUI ----

    public static void openNbtEditor(Player player, FunstartPlugin plugin) {
        openNbtEditor(player, plugin, 0);
    }

    public static void openNbtEditor(Player player, FunstartPlugin plugin, int page) {
        Inventory inv = Bukkit.createInventory(new NbtEditHolder(player, page), 54, "§b§lNBT 修改");

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            inv.setItem(4, makeItem(Material.BARRIER, "§c请手持物品后重试"));
        } else {
            inv.setItem(4, held.clone());
        }

        List<String> allKeys = getTagKeys(held, plugin);
        int totalPages = Math.max(1, (int) Math.ceil((double) allKeys.size() / TAGS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;

        int start = page * TAGS_PER_PAGE;
        int end = Math.min(start + TAGS_PER_PAGE, allKeys.size());
        List<String> pageKeys = allKeys.subList(start, end);

        // Tag slots
        int slot = 9;
        for (String key : pageKeys) {
            String value = getTagValue(held, plugin, key);
            ItemStack tagItem = makeItem(Material.NAME_TAG, "§b" + key,
                "§7值: §f" + (value != null ? value : "§cnull"),
                "",
                "§c点击移除此标签");
            inv.setItem(slot++, tagItem);
        }

        // Decoration
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§7NBT 修改");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane.clone());
        }

        // Navigation
        inv.setItem(48, page > 0 ? makeItem(Material.ARROW, "§e上一页") : makeItem(Material.GRAY_STAINED_GLASS_PANE, ""));
        inv.setItem(49, makeItem(Material.BOOK, "§7第 §e" + (page + 1) + " §7页 / §e" + totalPages + " §7页",
            "§7标签数: §e" + allKeys.size()));
        inv.setItem(50, page + 1 < totalPages ? makeItem(Material.ARROW, "§e下一页") : makeItem(Material.GRAY_STAINED_GLASS_PANE, ""));

        inv.setItem(51, makeItem(Material.LIME_DYE, "§a§l添加标签",
            "§7点击后在聊天框输入标签名",
            "§7然后输入标签值"));

        inv.setItem(53, makeItem(Material.BARRIER, "§c关闭"));

        player.openInventory(inv);
    }

    // ---- Event handlers ----

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof NbtEditHolder holder)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        int page = holder.getPage();

        // Navigation
        if (slot == 48 && page > 0) { openNbtEditor(player, plugin, page - 1); return; }
        if (slot == 50) { openNbtEditor(player, plugin, page + 1); return; }

        // Close
        if (slot == 53) { player.closeInventory(); return; }

        // Add tag
        if (slot == 51) {
            player.closeInventory();
            pendingTagKeys.remove(player.getUniqueId());
            plugin.addPendingChatAction(player.getUniqueId(),
                FunstartPlugin.PendingChatAction.Type.NBT_ADD_KEY, null);
            player.sendMessage("§b[NBT] §a请输入标签名 (字母数字下划线, 1-32字符):");
            player.sendMessage("§7输入任意其他内容取消");
            return;
        }

        // Clicking a tag -> remove it
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage("§c手持物品不能为空");
            player.closeInventory();
            return;
        }

        int index = slotToTagIndex(slot);
        if (index < 0) return;

        List<String> allKeys = getTagKeys(held, plugin);
        int realIndex = page * TAGS_PER_PAGE + index;
        if (realIndex < 0 || realIndex >= allKeys.size()) return;

        String key = allKeys.get(realIndex);
        removeTag(held, plugin, key);
        player.getInventory().setItemInMainHand(held);
        plugin.getLogger().info("[Admin] " + player.getName() + " 移除了NBT标签 " + key);
        player.sendMessage("§b[NBT] §a已移除标签: §e" + key);
        openNbtEditor(player, plugin, page);
    }

    private static int slotToTagIndex(int slot) {
        if (slot >= 9 && slot <= 17) return slot - 9;
        if (slot >= 18 && slot <= 26) return slot - 18 + 9;
        if (slot >= 27 && slot <= 35) return slot - 27 + 18;
        if (slot >= 36 && slot <= 44) return slot - 36 + 27;
        return -1;
    }

    // ---- Chat flow helpers ----

    public static Map<UUID, String> getPendingTagKeys() { return pendingTagKeys; }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof NbtEditHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {}

    // ---- Holder ----

    public static class NbtEditHolder implements InventoryHolder {
        private final Player player;
        private final int page;

        public NbtEditHolder(Player player, int page) {
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
}
