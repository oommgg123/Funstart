/*
 * 糖 — FSTFood
 * 食用方式: 手持 + 蹲下蓄力 1.7s
 * 效果: 中毒II(30s) 瞬间伤害 缓慢I(15s) 反胃I(15s)
 */

package moe.hinakusoft.funstart.custom.handler;

import moe.hinakusoft.funstart.api.FoodHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CandyFood implements FoodHandler {

    @Override
    public String getId() { return "candy"; }

    @Override
    public int getChargeTicks() { return 34; }

    @Override
    public void onEat(Player player, ItemStack item) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 600, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 1));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
    }
}
