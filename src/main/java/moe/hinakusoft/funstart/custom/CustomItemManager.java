package moe.hinakusoft.funstart.custom;

import moe.hinakusoft.funstart.FunstartPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class CustomItemManager {

    private final List<CustomItem> items = new ArrayList<>();
    private BukkitTask task;

    public void register(CustomItem item) {
        items.add(item);
    }

    public void startTicking(FunstartPlugin plugin) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (int slot = 0; slot <= 40; slot++) {
                    ItemStack item = getItemBySlot(player, slot);
                    if (item == null || item.getType().isAir()) continue;
                    for (CustomItem ci : items) {
                        if (ci.isItem(item)) {
                            ci.onTick(player, item, slot);
                        }
                    }
                }
            }
        }, 0L, 3L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private ItemStack getItemBySlot(Player player, int slot) {
        if (slot >= 0 && slot <= 39) {
            return player.getInventory().getItem(slot);
        } else if (slot == 40) {
            return player.getInventory().getItemInOffHand();
        }
        return null;
    }
}
