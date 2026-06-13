package moe.hinakusoft.funstart.custom.handler;

import moe.hinakusoft.funstart.api.FoodHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DefaultFood implements FoodHandler {

    @Override
    public String getId() { return "food"; }

    @Override
    public int getChargeTicks() { return 50; }

    @Override
    public void onEat(Player player, ItemStack item) {
        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
        player.setSaturation(Math.min(20, player.getSaturation() + 6));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
    }
}
