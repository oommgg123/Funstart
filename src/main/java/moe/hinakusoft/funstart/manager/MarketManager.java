package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.ItemStackData;
import moe.hinakusoft.funstart.model.MarketItem;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MarketManager {

    private final FunstartPlugin plugin;
    private final List<MarketItem> items = new ArrayList<>();
    private final Map<UUID, Double> pendingPoints = new HashMap<>();
    private final Map<UUID, List<ItemStackData>> pendingItems = new HashMap<>();
    private final Map<String, Double> lastTradePrice = new HashMap<>();

    public MarketManager(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "market.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> raw = config.getMapList("items");
        if (raw != null) {
            items.clear();
            for (Map<?, ?> m : raw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) m;
                items.add(MarketItem.deserialize(typed));
            }
        }
        // Load pending points
        if (config.contains("pendingPoints")) {
            for (String key : config.getConfigurationSection("pendingPoints").getKeys(false)) {
                pendingPoints.put(UUID.fromString(key), config.getDouble("pendingPoints." + key));
            }
        }
        // Load pending items
        if (config.contains("pendingItems")) {
            for (String key : config.getConfigurationSection("pendingItems").getKeys(false)) {
                UUID uid = UUID.fromString(key);
                List<Map<?, ?>> itemRaw = config.getMapList("pendingItems." + key);
                List<ItemStackData> itemList = new ArrayList<>();
                if (itemRaw != null) {
                    for (Map<?, ?> m : itemRaw) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typed = (Map<String, Object>) m;
                        itemList.add(ItemStackData.deserialize(typed));
                    }
                }
                pendingItems.put(uid, itemList);
            }
        }
        // Load last trade prices
        if (config.contains("lastTradePrice")) {
            for (String key : config.getConfigurationSection("lastTradePrice").getKeys(false)) {
                lastTradePrice.put(key, config.getDouble("lastTradePrice." + key));
            }
        }
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "market.yml");
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> raw = new ArrayList<>();
        for (MarketItem item : items) raw.add(item.serialize());
        config.set("items", raw);
        // Save pending points
        for (Map.Entry<UUID, Double> e : pendingPoints.entrySet()) {
            if (e.getValue() > 0) config.set("pendingPoints." + e.getKey().toString(), e.getValue());
        }
        // Save pending items
        for (Map.Entry<UUID, List<ItemStackData>> e : pendingItems.entrySet()) {
            if (!e.getValue().isEmpty()) {
                List<Map<String, Object>> itemRaw = new ArrayList<>();
                for (ItemStackData d : e.getValue()) itemRaw.add(d.serialize());
                config.set("pendingItems." + e.getKey().toString(), itemRaw);
            }
        }
        // Save last trade prices
        for (Map.Entry<String, Double> e : lastTradePrice.entrySet()) {
            config.set("lastTradePrice." + e.getKey(), e.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存市场数据: " + e.getMessage());
        }
    }

    public void addItem(MarketItem item) {
        items.add(item);
        save();
    }

    public void removeItem(MarketItem item) {
        items.remove(item);
        save();
    }

    public List<MarketItem> getItems() { return items; }

    public List<MarketItem> getOpShopItems() {
        List<MarketItem> result = new ArrayList<>();
        for (MarketItem item : items) {
            if (item.getType() == MarketItem.Type.OP_SHOP) result.add(item);
        }
        return result;
    }

    public List<MarketItem> getPlayerListings() {
        List<MarketItem> result = new ArrayList<>();
        for (MarketItem item : items) {
            if (item.getType() == MarketItem.Type.PLAYER_LISTING) result.add(item);
        }
        return result;
    }

    public List<MarketItem> getPlayerListings(UUID sellerId) {
        List<MarketItem> result = new ArrayList<>();
        for (MarketItem item : items) {
            if (item.getType() == MarketItem.Type.PLAYER_LISTING && item.getSellerId().equals(sellerId)) {
                result.add(item);
            }
        }
        return result;
    }

    public MarketItem getItemBySlot(int slot) {
        if (slot < 0 || slot >= items.size()) return null;
        return items.get(slot);
    }

    /** Check and apply restocks for OP shop items. Call periodically. */
    public void tickRestocks() {
        boolean changed = false;
        for (MarketItem item : items) {
            if (item.getType() == MarketItem.Type.OP_SHOP && item.needsRestock()) {
                item.applyRestock();
                changed = true;
            }
        }
        // Handle expired player listings → return remaining stock to seller's pending
        Iterator<MarketItem> it = items.iterator();
        while (it.hasNext()) {
            MarketItem item = it.next();
            if (item.getType() == MarketItem.Type.PLAYER_LISTING && item.isExpired()) {
                int remaining = item.getStock();
                if (remaining > 0) {
                    ItemStackData remainingData = new ItemStackData(
                            item.getItemData().toItemStack().asQuantity(remaining));
                    addPendingItem(item.getSellerId(), remainingData);
                }
                it.remove();
                changed = true;
            }
        }
        if (changed) save();
    }

    // ---- Pending points (seller offline earnings) ----

    public boolean hasPendingRewards(UUID playerId) {
        if (pendingPoints.getOrDefault(playerId, 0.0) > 0) return true;
        List<ItemStackData> items = pendingItems.get(playerId);
        return items != null && !items.isEmpty();
    }

    public double getPendingPointsTotal(UUID playerId) {
        return pendingPoints.getOrDefault(playerId, 0.0);
    }

    public void addPendingPoints(UUID playerId, double amount) {
        pendingPoints.merge(playerId, amount, Double::sum);
        save();
    }

    /** Claim all pending points. Returns the amount claimed. */
    public double claimPendingPoints(UUID playerId) {
        double amount = pendingPoints.remove(playerId);
        if (amount > 0) save();
        return amount;
    }

    public List<ItemStackData> getPendingItems(UUID playerId) {
        return pendingItems.getOrDefault(playerId, new ArrayList<>());
    }

    public void addPendingItem(UUID sellerId, ItemStackData itemData) {
        pendingItems.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(itemData);
        save();
    }

    /** Claim one pending item. Returns the ItemStackData, or null if none. */
    public ItemStackData claimOnePendingItem(UUID playerId) {
        List<ItemStackData> list = pendingItems.get(playerId);
        if (list == null || list.isEmpty()) return null;
        ItemStackData data = list.remove(0);
        if (list.isEmpty()) pendingItems.remove(playerId);
        save();
        return data;
    }

    public int getPendingItemCount(UUID playerId) {
        List<ItemStackData> list = pendingItems.get(playerId);
        return list == null ? 0 : list.size();
    }

    // ---- Price info ----

    /** Get average market price for a given item data string (for player info) */
    public String getMarketPriceInfo(String serialized) {
        double total = 0;
        int count = 0;
        for (MarketItem item : items) {
            if (item.getItemData().serialize().get("s").equals(serialized) && item.getStock() > 0) {
                total += item.getCurrentPrice();
                count++;
            }
        }
        if (count == 0) return "§7暂无市场价";
        return String.format("§7市场均价: §e%.1f §7点数", total / count);
    }

    /**
     * Get last trade price for an item type (by serialized string)
     */
    public double getLastTradePrice(String serialized) {
        return lastTradePrice.getOrDefault(serialized, -1.0);
    }

    /**
     * Record a trade to update last trade price
     */
    public void recordTrade(String serialized, double price) {
        lastTradePrice.put(serialized, price);
    }

    /**
     * Delist a player's own listing and return items to seller's pending
     */
    public void delistPlayerListing(MarketItem item) {
        int remaining = item.getStock();
        if (remaining > 0) {
            ItemStackData remainingData = new ItemStackData(
                    item.getItemData().toItemStack().asQuantity(remaining));
            addPendingItem(item.getSellerId(), remainingData);
        }
        removeItem(item);
    }
}
