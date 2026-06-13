package moe.hinakusoft.funstart.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MarketItem {

    public enum Type { OP_SHOP, PLAYER_LISTING }

    private Type type;
    private UUID sellerId;
    private String sellerName;
    private ItemStackData itemData;
    private double basePrice;
    private double currentPrice;
    private int stock;
    private int maxStock; // daily count for OP, original count for player
    private int priceUpCount;
    private int priceDownCount;
    private double ifBuyAllRestoreUp;  // %
    private double ifNotBuyAllRestoreDown; // %
    private long restoreInterval;  // hours -> millis
    private long lastRestoreTime;
    private long expireTime; // for player listings (0 = never)
    private long listedTime;

    // Player listing fields
    private int originalQuantity;
    private int soldQuantity;

    public MarketItem() {}

    // OP Shop constructor
    public static MarketItem createOpShop(ItemStackData itemData, double baseValue,
                                          int dailyCount, double buyAllUp, double notBuyAllDown,
                                          long restoreIntervalHours) {
        MarketItem m = new MarketItem();
        m.type = Type.OP_SHOP;
        m.sellerId = new UUID(0, 0);
        m.sellerName = "服务器商店";
        m.itemData = itemData;
        m.basePrice = baseValue;
        m.currentPrice = baseValue;
        m.stock = dailyCount;
        m.maxStock = dailyCount;
        m.ifBuyAllRestoreUp = buyAllUp;
        m.ifNotBuyAllRestoreDown = notBuyAllDown;
        m.restoreInterval = restoreIntervalHours * 3600000L;
        m.lastRestoreTime = System.currentTimeMillis();
        m.listedTime = System.currentTimeMillis();
        return m;
    }

    // Player listing constructor
    public static MarketItem createPlayerListing(UUID sellerId, String sellerName,
                                                  ItemStackData itemData, double pricePerUnit,
                                                  int quantity, long durationHours) {
        MarketItem m = new MarketItem();
        m.type = Type.PLAYER_LISTING;
        m.sellerId = sellerId;
        m.sellerName = sellerName;
        m.itemData = itemData;
        m.basePrice = pricePerUnit;
        m.currentPrice = pricePerUnit;
        m.stock = quantity;
        m.maxStock = quantity;
        m.originalQuantity = quantity;
        m.soldQuantity = 0;
        m.listedTime = System.currentTimeMillis();
        if (durationHours > 0) {
            m.expireTime = System.currentTimeMillis() + durationHours * 3600000L;
        }
        return m;
    }

    public void applyRestock() {
        if (type != Type.OP_SHOP) return;
        if (stock <= 0 && priceDownCount < 3) {
            // All sold out - price up
            currentPrice = currentPrice * (1 + ifBuyAllRestoreUp / 100.0);
            priceUpCount++;
        } else if (stock > 0 && priceUpCount < 3) {
            // Not all sold - price down
            currentPrice = currentPrice * (1 - ifNotBuyAllRestoreDown / 100.0);
            if (currentPrice < basePrice * 0.1) currentPrice = basePrice * 0.1;
            priceDownCount++;
        }
        stock = maxStock;
        lastRestoreTime = System.currentTimeMillis();
    }

    public boolean needsRestock() {
        if (type != Type.OP_SHOP) return false;
        if (restoreInterval <= 0) return false;
        return System.currentTimeMillis() - lastRestoreTime >= restoreInterval;
    }

    public boolean isExpired() {
        if (type != Type.PLAYER_LISTING) return false;
        return expireTime > 0 && System.currentTimeMillis() >= expireTime;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.name());
        map.put("sellerId", sellerId.toString());
        map.put("sellerName", sellerName);
        map.put("itemData", itemData.serialize());
        map.put("basePrice", basePrice);
        map.put("currentPrice", currentPrice);
        map.put("stock", stock);
        map.put("maxStock", maxStock);
        map.put("priceUpCount", priceUpCount);
        map.put("priceDownCount", priceDownCount);
        map.put("ifBuyAllRestoreUp", ifBuyAllRestoreUp);
        map.put("ifNotBuyAllRestoreDown", ifNotBuyAllRestoreDown);
        map.put("restoreInterval", restoreInterval);
        map.put("lastRestoreTime", lastRestoreTime);
        map.put("expireTime", expireTime);
        map.put("listedTime", listedTime);
        map.put("originalQuantity", originalQuantity);
        map.put("soldQuantity", soldQuantity);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static MarketItem deserialize(Map<String, Object> map) {
        MarketItem m = new MarketItem();
        m.type = Type.valueOf((String) map.get("type"));
        m.sellerId = UUID.fromString((String) map.get("sellerId"));
        m.sellerName = (String) map.get("sellerName");
        m.itemData = ItemStackData.deserialize((Map<String, Object>) map.get("itemData"));
        m.basePrice = ((Number) map.get("basePrice")).doubleValue();
        m.currentPrice = ((Number) map.get("currentPrice")).doubleValue();
        m.stock = ((Number) map.get("stock")).intValue();
        m.maxStock = ((Number) map.get("maxStock")).intValue();
        m.priceUpCount = ((Number) map.get("priceUpCount")).intValue();
        m.priceDownCount = ((Number) map.get("priceDownCount")).intValue();
        m.ifBuyAllRestoreUp = ((Number) map.get("ifBuyAllRestoreUp")).doubleValue();
        m.ifNotBuyAllRestoreDown = ((Number) map.get("ifNotBuyAllRestoreDown")).doubleValue();
        m.restoreInterval = ((Number) map.get("restoreInterval")).longValue();
        m.lastRestoreTime = ((Number) map.get("lastRestoreTime")).longValue();
        m.expireTime = map.containsKey("expireTime") ? ((Number) map.get("expireTime")).longValue() : 0;
        m.listedTime = map.containsKey("listedTime") ? ((Number) map.get("listedTime")).longValue() : 0;
        m.originalQuantity = map.containsKey("originalQuantity") ? ((Number) map.get("originalQuantity")).intValue() : m.stock;
        m.soldQuantity = map.containsKey("soldQuantity") ? ((Number) map.get("soldQuantity")).intValue() : 0;
        return m;
    }

    // Getters
    public Type getType() { return type; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public ItemStackData getItemData() { return itemData; }
    public double getBasePrice() { return basePrice; }
    public double getCurrentPrice() { return currentPrice; }
    public int getStock() { return stock; }
    public int getMaxStock() { return maxStock; }
    public int getPriceUpCount() { return priceUpCount; }
    public int getPriceDownCount() { return priceDownCount; }
    public double getIfBuyAllRestoreUp() { return ifBuyAllRestoreUp; }
    public double getIfNotBuyAllRestoreDown() { return ifNotBuyAllRestoreDown; }
    public long getRestoreInterval() { return restoreInterval; }
    public long getLastRestoreTime() { return lastRestoreTime; }
    public long getExpireTime() { return expireTime; }
    public long getListedTime() { return listedTime; }

    public void setStock(int stock) { this.stock = stock; }
    public void setSoldQuantity(int sq) { this.soldQuantity = sq; }
    public int getSoldQuantity() { return soldQuantity; }
}
