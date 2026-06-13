/*
 * FSTFood — 自定义食物系统
 * 所有 FSTFood 类食物食用方式: 手持物品 + 蹲下蓄力
 * 不同食物蓄力时间不同, 蓄力满后自动食用
 * 食物行为通过 FoodHandler 注册, 参见 handler/ 目录
 */

package moe.hinakusoft.funstart.custom;

import moe.hinakusoft.funstart.api.FoodHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FSTFood extends CustomItem {

    private final NamespacedKey typeKey;
    private final Map<String, FoodHandler> handlers = new ConcurrentHashMap<>();
    private final Map<UUID, ChargeData> charging = new ConcurrentHashMap<>();
    private FoodHandler defaultHandler;

    public FSTFood(NamespacedKey key) {
        super(key);
        this.typeKey = new NamespacedKey(key.getNamespace(), key.getKey() + "_type");
    }

    public void registerHandler(FoodHandler handler) {
        handlers.put(handler.getId(), handler);
    }

    public void setDefaultHandler(FoodHandler handler) {
        this.defaultHandler = handler;
        handlers.put(handler.getId(), handler);
    }

    public static boolean tagItem(ItemStack item, NamespacedKey key) {
        return tagItem(item, key, "food");
    }

    public static boolean tagItem(ItemStack item, NamespacedKey key, String type) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
        if (type != null && !type.isEmpty() && !type.equals("food")) {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(key.getNamespace(), key.getKey() + "_type"),
                PersistentDataType.STRING, type);
        }
        item.setItemMeta(meta);
        return true;
    }

    @Override
    public boolean isItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(key, PersistentDataType.BOOLEAN);
    }

    @Override
    public void onTick(Player player, ItemStack item, int slot) {
        // 仅处理主手物品
        if (player.getInventory().getHeldItemSlot() != slot) return;

        if (!player.isSneaking()) {
            cancelCharge(player);
            return;
        }

        ChargeData data = charging.get(player.getUniqueId());
        if (data == null) {
            data = new ChargeData();
            data.slot = slot;
            String type = item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(typeKey, PersistentDataType.STRING, "food");
            data.type = type;
            FoodHandler h = handlers.get(type);
            data.maxTicks = h != null ? h.getChargeTicks() : 50;
            charging.put(player.getUniqueId(), data);
        }

        if (data.slot != slot) {
            cancelCharge(player);
            return;
        }

        data.ticks++;
        int progress = Math.min(data.ticks * 100 / data.maxTicks, 100);
        String bar = buildProgressBar(progress);
        player.sendActionBar("§6食用" + stripColor(item.getItemMeta().getDisplayName()) + "中 " + bar + " §7" + progress + "%");

        if (data.ticks >= data.maxTicks) {
            consume(player, item, data.type);
            cancelCharge(player);
        }
    }

    private void consume(Player player, ItemStack item, String type) {
        if (item.getAmount() <= 0) return;
        item.setAmount(item.getAmount() - 1);

        FoodHandler handler = handlers.get(type);
        if (handler != null) {
            handler.onEat(player, item);
        } else if (defaultHandler != null) {
            defaultHandler.onEat(player, item);
        }
    }

    private void cancelCharge(Player player) {
        ChargeData removed = charging.remove(player.getUniqueId());
        if (removed != null) {
            player.sendActionBar("");
        }
    }

    private static String buildProgressBar(int percent) {
        int filled = percent / 10;
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < filled; i++) sb.append("█");
        sb.append("§7");
        for (int i = filled; i < 10; i++) sb.append("█");
        return sb.toString();
    }

    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    private static class ChargeData {
        int ticks;
        int slot;
        String type;
        int maxTicks;
    }
}
