package moe.hinakusoft.funstart.listener;

import java.util.List;
import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.manager.DailyTaskManager;
import moe.hinakusoft.funstart.manager.DailyTaskManager.TaskDef;
import moe.hinakusoft.funstart.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DailyTaskListener implements Listener {

    private final FunstartPlugin plugin;

    public DailyTaskListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    public static void openFor(Player player, FunstartPlugin plugin) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        DailyTaskManager.checkAndReset(data);

        Inventory inv = Bukkit.createInventory(new TaskHolder(player), 27, "§6§l每日任务");

        for (int i = 0; i < 3; i++) {
            TaskDef task = DailyTaskManager.TASKS.get(i);
            int progress = data.getTaskProgress()[i];
            boolean completed = data.getTaskCompleted()[i];
            int target = task.target();

            Material mat = switch (i) {
                case 0 -> Material.DIAMOND_PICKAXE;
                case 1 -> Material.ZOMBIE_HEAD;
                case 2 -> Material.EXPERIENCE_BOTTLE;
                default -> Material.PAPER;
            };

            double pct = (double) progress / target * 100;
            String bar = makeBar(pct, 20);

            ItemStack item = new ItemStack(completed ? Material.SUNFLOWER : mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((completed ? "§a§l✔ " : "§6§l") + task.name());
            meta.setLore(List.of(
                "§7" + task.description(),
                "",
                "§7进度: §e" + progress + "§7/§e" + target,
                "§7" + bar + " §f" + String.format("%.0f", pct) + "%",
                "",
                completed ? "§e点击领取 §6" + PlayerData.fmt(task.reward()) + " §e点" : "§7进行中..."
            ));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§7任务日期: §f" + DailyTaskManager.getTodayDate());
        infoMeta.setLore(List.of("§7每日0点刷新", "§7完成所有任务可获得额外奖励!"));
        info.setItemMeta(infoMeta);
        inv.setItem(26, info);

        player.openInventory(inv);
    }

    private static String makeBar(double pct, int length) {
        int filled = (int) (pct / 100 * length);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < length; i++) {
            if (i == filled) sb.append("§7");
            sb.append("■");
        }
        return sb.toString();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TaskHolder holder)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 3) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        DailyTaskManager.checkAndReset(data);

        if (!data.getTaskCompleted()[slot]) {
            player.sendMessage("§c该任务尚未完成");
            return;
        }

        if (DailyTaskManager.claimReward(data, slot)) {
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
            TaskDef task = DailyTaskManager.TASKS.get(slot);
            player.sendMessage("§e[Funstart] §a领取了每日任务奖励 §6" + PlayerData.fmt(task.reward()) + " §a点");
            openFor(player, plugin);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TaskHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {}

    public static class TaskHolder implements InventoryHolder {
        private final Player player;
        public TaskHolder(Player player) { this.player = player; }
        public Player getPlayer() { return player; }
        @Override public Inventory getInventory() { return null; }
    }
}
