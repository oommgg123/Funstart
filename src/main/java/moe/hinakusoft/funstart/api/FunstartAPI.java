package moe.hinakusoft.funstart.api;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.custom.CustomItemManager;
import moe.hinakusoft.funstart.custom.FSTFood;
import moe.hinakusoft.funstart.manager.ClaimManager;
import moe.hinakusoft.funstart.manager.FSTActionBar;
import moe.hinakusoft.funstart.manager.PlayerDataManager;
import moe.hinakusoft.funstart.manager.TpaManager;
import moe.hinakusoft.funstart.manager.WarpManager;
import moe.hinakusoft.funstart.model.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class FunstartAPI {

    private static FunstartPlugin plugin;

    private FunstartAPI() {}

    public static void init(FunstartPlugin p) {
        plugin = p;
    }

    public static FunstartPlugin getPlugin() {
        return plugin;
    }

    // ---- Manager access ----

    public static PlayerDataManager getPlayerDataManager() {
        return plugin.getPlayerDataManager();
    }

    public static TpaManager getTpaManager() {
        return plugin.getTpaManager();
    }

    public static WarpManager getWarpManager() {
        return plugin.getWarpManager();
    }

    public static ClaimManager getClaimManager() {
        return plugin.getClaimManager();
    }

    public static CustomItemManager getCustomItemManager() {
        return plugin.getCustomItemManager();
    }

    public static FSTActionBar getActionBar() {
        return plugin.getActionBar();
    }

    // ---- Player points ----

    public static double getPoints(Player player) {
        return plugin.getPlayerDataManager().getPlayerData(player).getPoints();
    }

    public static void addPoints(Player player, double amount) {
        plugin.getPlayerDataManager().getPlayerData(player).addPoints(amount);
    }

    public static boolean deductPoints(Player player, double amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data.getPoints() < amount) return false;
        data.deductPoints(amount);
        return true;
    }

    // ---- FSTFood ----

    public static void registerFoodHandler(FoodHandler handler) {
        plugin.getFstFood().registerHandler(handler);
    }

    public static boolean tagAsFSTFood(ItemStack item, String type) {
        return FSTFood.tagItem(item, new NamespacedKey(plugin, "fst_food"), type);
    }

    public static boolean isFSTFood(ItemStack item) {
        return plugin.getFstFood().isItem(item);
    }

    // ---- Custom Item registration ----

    public static void registerCustomItem(FSTFood item) {
        plugin.getCustomItemManager().register(item);
    }

    // ---- JavaPlugin reference ----

    public static JavaPlugin getJavaPlugin() {
        return plugin;
    }
}
