/*
 * 肉类杂炖 — FSTFood
 * 食用方式: 手持 + 蹲下蓄力 2.0s
 * 效果: 抗性提升II(20s) 速度I(5s) 恢复16饱食度
 * 合成: 牛肉(A) 鸡肉(B) 羊肉(C) 猪肉(D) 按AC/BD 产出2个
 */

package moe.hinakusoft.funstart.custom.handler;

import moe.hinakusoft.funstart.api.FoodHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MeatStewFood implements FoodHandler {

    @Override
    public String getId() { return "stew"; }

    @Override
    public int getChargeTicks() { return 40; }

    @Override
    public void onEat(Player player, ItemStack item) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0));
        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 16));
        player.setSaturation(Math.min(20, player.getSaturation() + 16));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
    }
}
