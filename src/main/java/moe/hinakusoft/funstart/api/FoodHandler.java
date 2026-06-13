package moe.hinakusoft.funstart.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface FoodHandler {
    String getId();
    int getChargeTicks();
    void onEat(Player player, ItemStack item);
}
