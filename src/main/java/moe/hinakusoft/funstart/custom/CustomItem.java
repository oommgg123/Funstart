package moe.hinakusoft.funstart.custom;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class CustomItem {

    protected final NamespacedKey key;

    public CustomItem(NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey getKey() { return key; }

    public abstract boolean isItem(ItemStack item);

    public abstract void onTick(Player player, ItemStack item, int slot);
}
