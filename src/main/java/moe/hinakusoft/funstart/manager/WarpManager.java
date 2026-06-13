package moe.hinakusoft.funstart.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.WarpPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class WarpManager {

    private final FunstartPlugin plugin;
    private final Map<String, WarpPoint> warps = new ConcurrentHashMap<>();
    private final Map<UUID, List<ShareRequest>> pendingShares = new ConcurrentHashMap<>();
    private File warpFile;

    public WarpManager(FunstartPlugin plugin) {
        this.plugin = plugin;
        startCleanupTimer();
    }

    public void load() {
        warpFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(warpFile);
        ConfigurationSection section = config.getConfigurationSection("warps");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection ws = section.getConfigurationSection(key);
            if (ws == null) continue;
            warps.put(key, new WarpPoint(
                key,
                ws.getString("name", key),
                UUID.fromString(ws.getString("owner")),
                ws.getString("world"),
                ws.getDouble("x"), ws.getDouble("y"), ws.getDouble("z"),
                (float) ws.getDouble("yaw"), (float) ws.getDouble("pitch")
            ));
        }
    }

    public void save() {
        if (warpFile == null) warpFile = new File(plugin.getDataFolder(), "warps.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, WarpPoint> e : warps.entrySet()) {
            WarpPoint w = e.getValue();
            String path = "warps." + e.getKey();
            config.set(path + ".name", w.getName());
            config.set(path + ".owner", w.getOwner().toString());
            config.set(path + ".world", w.getWorldName());
            config.set(path + ".x", w.getX());
            config.set(path + ".y", w.getY());
            config.set(path + ".z", w.getZ());
            config.set(path + ".yaw", (double) w.getYaw());
            config.set(path + ".pitch", (double) w.getPitch());
        }
        try { config.save(warpFile); }
        catch (IOException ex) {
            plugin.getLogger().warning("无法保存 warps.yml");
        }
    }

    public boolean createWarp(String name, UUID owner, Location loc) {
        String id = name.toLowerCase() + "_" + owner;
        if (warps.containsKey(id)) return false;
        WarpPoint wp = new WarpPoint(id, name, owner,
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
            loc.getYaw(), loc.getPitch());
        warps.put(id, wp);
        save();
        return true;
    }

    public WarpPoint getWarp(String id) {
        return warps.get(id);
    }

    public WarpPoint findWarpByName(String name, UUID owner) {
        WarpPoint w = warps.get(name.toLowerCase() + "_" + owner);
        if (w != null) return w;
        for (WarpPoint wp : warps.values()) {
            if (wp.getName().equalsIgnoreCase(name)) return wp;
        }
        return null;
    }

    public List<WarpPoint> getOwnWarps(UUID owner) {
        List<WarpPoint> list = new ArrayList<>();
        for (WarpPoint w : warps.values()) {
            if (w.getOwner().equals(owner)) list.add(w);
        }
        return list;
    }

    public List<WarpPoint> getAvailableWarps(Player player) {
        Set<String> accepted = plugin.getPlayerDataManager()
            .getPlayerData(player).getAcceptedShares();
        List<WarpPoint> list = new ArrayList<>();
        for (WarpPoint w : warps.values()) {
            if (w.getOwner().equals(player.getUniqueId()) || accepted.contains(w.getId())) {
                list.add(w);
            }
        }
        return list;
    }

    public boolean deleteWarp(String id, UUID owner) {
        WarpPoint w = warps.get(id);
        if (w == null || !w.getOwner().equals(owner)) return false;
        warps.remove(id);
        save();
        return true;
    }

    public boolean warpExists(String name, UUID owner) {
        return warps.containsKey(name.toLowerCase() + "_" + owner);
    }

    public boolean sendShareRequest(UUID from, UUID to, String warpId) {
        if (from.equals(to)) return false;
        List<ShareRequest> list = pendingShares.computeIfAbsent(to, k -> new ArrayList<>());
        for (ShareRequest sr : list) {
            if (sr.from.equals(from) && sr.warpId.equals(warpId)) return true;
        }
        list.add(new ShareRequest(from, warpId, System.currentTimeMillis()));
        return true;
    }

    public List<ShareRequest> getPendingShares(UUID target) {
        return pendingShares.getOrDefault(target, new ArrayList<>());
    }

    public ShareRequest acceptShare(UUID target, UUID fromPlayer) {
        List<ShareRequest> list = pendingShares.get(target);
        if (list == null) return null;
        Iterator<ShareRequest> it = list.iterator();
        while (it.hasNext()) {
            ShareRequest sr = it.next();
            if (sr.from.equals(fromPlayer)) {
                it.remove();
                if (list.isEmpty()) pendingShares.remove(target);
                return sr;
            }
        }
        return null;
    }

    public void denyShare(UUID target, UUID fromPlayer) {
        List<ShareRequest> list = pendingShares.get(target);
        if (list == null) return;
        list.removeIf(sr -> sr.from.equals(fromPlayer));
        if (list.isEmpty()) pendingShares.remove(target);
    }

    private void startCleanupTimer() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long expireTime = 120_000L; // 120 seconds TTL
            pendingShares.values().forEach(list ->
                list.removeIf(sr -> now - sr.getTimestamp() > expireTime)
            );
            pendingShares.values().removeIf(List::isEmpty);
        }, 600L, 600L); // every 30 seconds
    }

    public static class ShareRequest {
        private final UUID from;
        private final String warpId;
        private final long timestamp;

        public ShareRequest(UUID from, String warpId, long timestamp) {
            this.from = from;
            this.warpId = warpId;
            this.timestamp = timestamp;
        }

        public UUID getFrom() { return from; }
        public String getWarpId() { return warpId; }
        public long getTimestamp() { return timestamp; }
    }
}
