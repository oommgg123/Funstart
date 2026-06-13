package moe.hinakusoft.funstart.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.ClaimRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class ClaimManager {

    private static final long FREE_VOLUME = 80L * 100 * 80;
    private static final double COST_PER_100 = 2.5;

    private final FunstartPlugin plugin;
    private final List<ClaimRegion> claims = new ArrayList<>();
    private final Map<UUID, ClaimSessionData> claimSessions = new HashMap<>();
    private Location spawnLocation;
    private int spawnRadius = 350;

    private static final long CLAIM_SESSION_TIMEOUT = 600_000L; // 10 min

    public ClaimManager(FunstartPlugin plugin) {
        this.plugin = plugin;
        World w = Bukkit.getWorlds().get(0);
        if (w != null) {
            spawnLocation = w.getSpawnLocation();
        }
    }

    // ---- Spawn Protection ----

    public boolean isInSpawnProtection(Location loc) {
        if (spawnLocation == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().equals(spawnLocation.getWorld())) return false;
        int dx = Math.abs(loc.getBlockX() - spawnLocation.getBlockX());
        int dz = Math.abs(loc.getBlockZ() - spawnLocation.getBlockZ());
        return dx <= spawnRadius && dz <= spawnRadius;
    }

    public boolean isInSpawnProtection(String worldName, int x, int y, int z) {
        if (spawnLocation == null) return false;
        if (!worldName.equals(spawnLocation.getWorld().getName())) return false;
        int dx = Math.abs(x - spawnLocation.getBlockX());
        int dz = Math.abs(z - spawnLocation.getBlockZ());
        return dx <= spawnRadius && dz <= spawnRadius;
    }

    // ---- Claim Queries ----

    public ClaimRegion getClaimAt(Location loc) {
        if (loc.getWorld() == null) return null;
        String wn = loc.getWorld().getName();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        for (ClaimRegion c : claims) {
            if (c.contains(wn, x, y, z)) return c;
        }
        return null;
    }

    public ClaimRegion getClaimByOwner(UUID owner) {
        for (ClaimRegion c : claims) {
            if (c.getOwner().equals(owner)) return c;
        }
        return null;
    }

    public boolean canBuild(Player player, Location loc) {
        if (player.isOp()) return true;
        if (isInSpawnProtection(loc)) return false;

        ClaimRegion claim = getClaimAt(loc);
        if (claim == null) return true; // no claim -> can build

        UUID uid = player.getUniqueId();
        return claim.getOwner().equals(uid) || claim.getTrustedPlayers().contains(uid);
    }

    // ---- Claim Creation ----

    public String createClaim(Player player, Location pos1, Location pos2) {
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            return "§c两个角点必须在同一世界";
        }
        String wn = pos1.getWorld().getName();
        int x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int y1 = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int x2 = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int y2 = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Check spawn protection
        if (isInSpawnProtection(wn, x1, y1, z1) || isInSpawnProtection(wn, x2, y2, z2)) {
            return "§c圈地区域与出生点保护区重叠";
        }

        // Check overlap with other claims
        ClaimRegion temp = new ClaimRegion(player.getUniqueId(), wn, x1, y1, z1, x2, y2, z2);
        for (ClaimRegion c : claims) {
            if (c.getOwner().equals(player.getUniqueId())) {
                return "§c你已经有一个圈地了, 最多只能有一个";
            }
            if (c.overlaps(temp)) {
                return "§c圈地区域与其他玩家领地重叠";
            }
        }

        long volume = temp.getVolume();
        if (volume < 2) {
            return "§c圈地面积太小 (至少2个方块)";
        }

        long extra = Math.max(0, volume - FREE_VOLUME);
        double cost = (extra / 100.0) * COST_PER_100;

        if (cost > 0) {
            var pd = plugin.getPlayerDataManager().getPlayerData(player);
            if (pd.getPoints() < cost) {
                return "§c点数不足! 圈地额外体积 " + extra + " 需要 " + String.format("%.1f", cost) + " 点, 你只有 " + String.format("%.1f", pd.getPoints()) + " 点";
            }
            pd.deductPoints(cost);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        }

        claims.add(temp);
        save();
        return null; // success
    }

    public void deleteClaim(UUID owner) {
        claims.removeIf(c -> c.getOwner().equals(owner));
        save();
    }

    public boolean addTrusted(UUID owner, UUID trusted) {
        ClaimRegion c = getClaimByOwner(owner);
        if (c == null) return false;
        if (c.getTrustedPlayers().contains(trusted)) return false;
        c.getTrustedPlayers().add(trusted);
        save();
        return true;
    }

    public boolean removeTrusted(UUID owner, UUID trusted) {
        ClaimRegion c = getClaimByOwner(owner);
        if (c == null) return false;
        if (!c.getTrustedPlayers().remove(trusted)) return false;
        save();
        return true;
    }

    // ---- Save/Load ----

    public void load() {
        File file = new File(plugin.getDataFolder(), "claims.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> raw = config.getMapList("claims");
        if (raw != null) {
            claims.clear();
            for (Map<?, ?> m : raw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) m;
                claims.add(ClaimRegion.deserialize(typed));
            }
        }
    }

    public int getSpawnRadius() { return spawnRadius; }

    // ---- Claim Creation Session (independent of pendingChatActions) ----

    public boolean hasActiveSession(UUID uid) {
        return claimSessions.containsKey(uid);
    }

    public void startClaimSession(Player player) {
        UUID uid = player.getUniqueId();
        plugin.getPendingWarpCreation().remove(uid);
        plugin.removePendingChatAction(uid); // clear old CLAIM_POSITION timeout
        claimSessions.put(uid, new ClaimSessionData());

        plugin.addPendingChatAction(uid,
            FunstartPlugin.PendingChatAction.Type.CLAIM_POSITION, null,
            12000L);
        player.sendMessage("§6[圈地] 请破坏第一个角方块, 然后在聊天框输入 §e1 §6确认");
        player.sendMessage("§7(只破坏不输入数字, 坐标不会提交)");
    }

    public static class ClaimSessionData {
        public boolean firstSet = false;
        public boolean secondSet = false;
        public String breakWorld;
        public String claimWorld;
        public int x1, y1, z1;
        public int x2, y2, z2;
        public long lastActivity = System.currentTimeMillis();
    }

    public void setPendingBreak(Player player, Location loc) {
        UUID uid = player.getUniqueId();
        ClaimSessionData csd = claimSessions.get(uid);
        if (csd == null) return;
        csd.breakWorld = loc.getWorld().getName();
        csd.lastActivity = System.currentTimeMillis();
        if (!csd.firstSet) {
            csd.x1 = loc.getBlockX();
            csd.y1 = loc.getBlockY();
            csd.z1 = loc.getBlockZ();
        } else {
            csd.x2 = loc.getBlockX();
            csd.y2 = loc.getBlockY();
            csd.z2 = loc.getBlockZ();
        }
    }

    public void handleGlobalClaimChat(Player player, String msg) {
        UUID uid = player.getUniqueId();
        ClaimSessionData csd = claimSessions.get(uid);
        if (csd == null) return;

        if (System.currentTimeMillis() - csd.lastActivity > CLAIM_SESSION_TIMEOUT) {
            claimSessions.remove(uid);
            plugin.removePendingChatAction(uid);
            player.sendMessage("§c圈地操作已超时");
            return;
        }

        if (msg.equals("1") && !csd.firstSet) {
            if (csd.breakWorld == null) {
                player.sendMessage("§c请先破坏一个方块后再输入 1");
                return;
            }
            csd.firstSet = true;
            csd.claimWorld = csd.breakWorld;
            player.sendMessage("§6[圈地] 角点1已确认: §e" + csd.x1 + ", " + csd.y1 + ", " + csd.z1);
            player.sendMessage("§6请破坏第二个角方块, 然后在聊天框输入 §e2 §6确认");
            csd.breakWorld = null;
            csd.lastActivity = System.currentTimeMillis();
            return;
        }

        if (msg.equals("2")) {
            if (!csd.firstSet) {
                player.sendMessage("§c请先确认角点1 (输入 1)");
                return;
            }
            if (csd.breakWorld == null) {
                player.sendMessage("§c请先破坏一个方块后再输入 2");
                return;
            }
            csd.secondSet = true;

            org.bukkit.Location loc1 = new org.bukkit.Location(
                Bukkit.getWorld(csd.claimWorld), csd.x1, csd.y1, csd.z1);
            org.bukkit.Location loc2 = new org.bukkit.Location(
                Bukkit.getWorld(csd.breakWorld), csd.x2, csd.y2, csd.z2);

            String result = createClaim(player, loc1, loc2);
            if (result == null) {
                player.sendMessage("§e[Funstart] §a领地创建成功!");
            } else {
                player.sendMessage(result);
            }
            claimSessions.remove(uid);
            plugin.removePendingChatAction(uid);
        }
    }

    // Kept for backward compat with CLAIM_POSITION switch case (delegates)
    public void handleClaimChat(Player player, String msg, FunstartPlugin.PendingChatAction action) {
        handleGlobalClaimChat(player, msg);
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "claims.yml");
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> raw = new ArrayList<>();
        for (ClaimRegion c : claims) raw.add(c.serialize());
        config.set("claims", raw);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存圈地数据: " + e.getMessage());
        }
    }
}
