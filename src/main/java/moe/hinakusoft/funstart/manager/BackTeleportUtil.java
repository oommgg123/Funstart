package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class BackTeleportUtil {

    public static void backToDeath(Player player, FunstartPlugin plugin) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        String locStr = data.getLastDeathLocation();
        if (locStr == null || locStr.isEmpty()) {
            player.sendMessage("§c没有记录到死亡地点");
            return;
        }
        Location loc = parseDeathLocation(locStr);
        if (loc == null || loc.getWorld() == null) {
            player.sendMessage("§c死亡地点所在世界不可用");
            return;
        }
        if (data.getPoints() < 5.0) {
            player.sendMessage("§c点数不足! 需要 5 点");
            return;
        }
        if (!tryTeleport(player, loc, 5.0, plugin)) return;
        player.sendMessage("§e[Funstart] §a已传送到上次死亡地点, 消耗 5 点, 剩余 §e" + PlayerData.fmt(data.getPoints()) + " §a点");
    }

    public static void backToHome(Player player, FunstartPlugin plugin) {
        Location loc = player.getRespawnLocation();
        if (loc == null) {
            player.sendMessage("§c你没有设置重生点");
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data.getPoints() < 3.0) {
            player.sendMessage("§c点数不足! 需要 3 点");
            return;
        }
        if (!tryTeleport(player, loc, 3.0, plugin)) return;
        player.sendMessage("§e[Funstart] §a已传送到重生点, 消耗 3 点, 剩余 §e" + PlayerData.fmt(data.getPoints()) + " §a点");
    }

    public static void backToWorld(Player player, FunstartPlugin plugin) {
        World world = Bukkit.getWorlds().get(0);
        Location loc = world.getSpawnLocation();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data.getPoints() < 3.0) {
            player.sendMessage("§c点数不足! 需要 3 点");
            return;
        }
        if (!tryTeleport(player, loc, 3.0, plugin)) return;
        player.sendMessage("§e[Funstart] §a已传送到主城, 消耗 3 点, 剩余 §e" + PlayerData.fmt(data.getPoints()) + " §a点");
    }

    private static boolean tryTeleport(Player player, Location loc, double cost, FunstartPlugin plugin) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        data.deductPoints(cost);
        double remaining = data.getPoints();
        if (plugin.hasPendingTeleport(player.getUniqueId())) {
            player.sendMessage("§c你已有正在进行的传送");
            data.addPoints(cost);
            return false;
        }
        plugin.setPendingTeleport(player.getUniqueId(), true);
        player.teleportAsync(loc).thenAccept(success -> {
            plugin.setPendingTeleport(player.getUniqueId(), false);
            if (!success) {
                data.addPoints(cost);
                plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
                player.sendMessage("§c传送失败，点数已退还");
            }
        });
        return true;
    }

    public static Location parseDeathLocation(String str) {
        String[] parts = str.split(",");
        if (parts.length != 6) return null;
        World world = Bukkit.getWorld(parts[0].trim());
        if (world == null) return null;
        try {
            return new Location(world,
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim()),
                Float.parseFloat(parts[4].trim()),
                Float.parseFloat(parts[5].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
