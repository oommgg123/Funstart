package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.ClaimRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LogManager {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    private static final Set<String> DANGEROUS = Set.of(
        "TNT", "TNT_MINECART", "RESPAWN_ANCHOR", "END_CRYSTAL",
        "FLINT_AND_STEEL", "FIRE_CHARGE", "LAVA_BUCKET", "LAVA",
        "WITHER_SKELETON_SKULL", "CREEPER_HEAD"
    );
    private static final Set<String> VALUABLE = Set.of(
        "DIAMOND", "DIAMOND_BLOCK", "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE",
        "NETHERITE_INGOT", "NETHERITE_BLOCK", "NETHERITE_SCRAP",
        "NETHERITE_SWORD", "NETHERITE_PICKAXE", "NETHERITE_AXE",
        "NETHERITE_SHOVEL", "NETHERITE_HOE", "NETHERITE_HELMET",
        "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS",
        "ENCHANTED_GOLDEN_APPLE", "TRIDENT", "ELYTRA",
        "SHULKER_BOX", "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX",
        "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX",
        "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX",
        "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX",
        "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX",
        "RED_SHULKER_BOX", "BLACK_SHULKER_BOX"
    );

    private static final String[] CATEGORIES = {"playerChunk", "playerMove", "playerJoin", "playerItem", "playerAttack"};

    private final FunstartPlugin plugin;
    private final File logsDir;
    private final Map<String, File> currentArchive = new HashMap<>();
    private final Map<String, Integer> archivePart = new HashMap<>();
    private final Map<String, BufferedWriter> openWriters = new HashMap<>();

    // Cached timestamp, updated every 20 ticks
    private String cachedNow = "";
    private int tickCounter = 0;

    // Attack tracking
    private final Map<UUID, List<Long>> attackTimes = new HashMap<>();
    private final Map<UUID, Set<UUID>> attackTargets = new HashMap<>();

    // Damage cooldown: skip logging same player damage within 500ms
    private final Map<UUID, Long> lastDamageLog = new HashMap<>();

    public LogManager(FunstartPlugin plugin) {
        this.plugin = plugin;
        this.logsDir = new File(plugin.getDataFolder(), "logs");
        initDirectories();
        startTickUpdater();
    }

    private void startTickUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            cachedNow = TS_FMT.format(new Date());
            tickCounter++;
            // Flush writers every 200 ticks (10s)
            if (tickCounter % 200 == 0) {
                flushAllWriters();
            }
        }, 1L, 1L);
    }

    private void flushAllWriters() {
        for (BufferedWriter w : openWriters.values()) {
            try { w.flush(); } catch (IOException ignored) {}
        }
    }

    private void closeAllWriters() {
        for (BufferedWriter w : openWriters.values()) {
            try { w.close(); } catch (IOException ignored) {}
        }
        openWriters.clear();
    }

    private void initDirectories() {
        for (String cat : CATEGORIES) {
            new File(logsDir, cat + "/latest").mkdirs();
            new File(logsDir, cat + "/archive").mkdirs();
            new File(logsDir, cat + "/other").mkdirs();
        }
    }

    private String now() {
        if (cachedNow.isEmpty()) cachedNow = TS_FMT.format(new Date());
        return cachedNow;
    }

    private String today() {
        return DATE_FMT.format(new Date());
    }

    // ---- File helpers ----

    private File getArchiveFile(String category) {
        String date = today();
        String key = category + "/" + date;
        File f = currentArchive.get(key);
        if (f != null && f.exists() && f.length() < MAX_FILE_SIZE) return f;

        File dir = new File(logsDir, category + "/archive");
        int part = archivePart.getOrDefault(key, 0) + 1;
        File candidate;
        do {
            String name = part == 1 ? date + ".log" : date + "_" + part + ".log";
            candidate = new File(dir, name);
            part++;
        } while (candidate.exists() && candidate.length() >= MAX_FILE_SIZE);

        archivePart.put(key, part - 1);
        currentArchive.put(key, candidate);
        return candidate;
    }

    private File getOtherFile(String category) {
        String date = today();
        return new File(new File(logsDir, category + "/other"), date + ".log");
    }

    private BufferedWriter getWriter(File file) {
        String path = file.getAbsolutePath();
        BufferedWriter w = openWriters.get(path);
        if (w != null) return w;

        try {
            file.getParentFile().mkdirs();
            w = new BufferedWriter(new FileWriter(file, true));
            openWriters.put(path, w);
            return w;
        } catch (IOException e) {
            plugin.getLogger().warning("日志打开失败 (" + file + "): " + e.getMessage());
            return null;
        }
    }

    private void appendLine(File file, String line) {
        try {
            BufferedWriter w = getWriter(file);
            if (w == null) return;
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("日志写入失败 (" + file + "): " + e.getMessage());
        }
    }

    private void logArchive(String category, String line) {
        appendLine(getArchiveFile(category), line);
    }

    private void logOther(String category, String line) {
        appendLine(getOtherFile(category), line);
    }

    /** Called on plugin disable: flush all writers, then copy latest archive to current.log */
    public void onDisable() {
        closeAllWriters();

        for (String cat : CATEGORIES) {
            File latestDir = new File(logsDir, cat + "/latest");
            File latestFile = new File(latestDir, "current.log");

            // Find the most recent archive file
            File archiveDir = new File(logsDir, cat + "/archive");
            File[] files = archiveDir.listFiles((d, n) -> n.endsWith(".log"));
            if (files == null || files.length == 0) {
                latestFile.delete();
                continue;
            }
            File newest = null;
            long newestMod = 0;
            for (File f : files) {
                if (f.lastModified() > newestMod) {
                    newestMod = f.lastModified();
                    newest = f;
                }
            }
            if (newest == null) continue;
            try {
                Files.copy(newest.toPath(), latestFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().warning("无法更新最新日志: " + e.getMessage());
            }
        }
    }

    // ========== playerChunk ==========

    public void logBlockBreak(Player player, Block block, boolean isOp, ItemStack tool) {
        try {
            String ts = now();
            Location loc = block.getLocation();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
            String coords = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            Material type = block.getType();
            String matName = type.name();
            String claimInfo = getClaimInfo(loc);

            String toolInfo = "空手";
            if (tool != null && tool.getType() != Material.AIR) {
                String fstId = plugin.getFstItemIdManager().getOrCreateItemId(tool);
                toolInfo = tool.getType().name() + " [fst:" + fstId + "]";
            }

            String line = String.format("[%s] %s [%s] 使用 %s 挖掘了 %s 在 %s (%s)%s",
                ts, player.getName(), player.getUniqueId(), toolInfo, matName, world, coords, claimInfo);
            logArchive("playerChunk", line);

            boolean isDangerous = DANGEROUS.contains(matName);
            boolean isValuable = VALUABLE.contains(matName);
            boolean noClaimPerm = claimInfo.isEmpty() && plugin.getClaimManager().getClaimAt(loc) != null && !isOp;
            boolean isContainer = isContainerBlock(type);
            boolean claimContainer = isContainer && !claimInfo.isEmpty();

            if (isDangerous || isValuable || noClaimPerm || claimContainer) {
                logOther("playerChunk", "§c[!] " + line);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("logBlockBreak 异常: " + e.getMessage());
        }
    }

    public void logBlockPlace(Player player, Block block, boolean isOp, ItemStack tool) {
        try {
            String ts = now();
            Location loc = block.getLocation();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
            String coords = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            Material type = block.getType();
            String matName = type.name();
            String claimInfo = getClaimInfo(loc);

            String toolInfo = "空手";
            if (tool != null && tool.getType() != Material.AIR) {
                String fstId = plugin.getFstItemIdManager().getOrCreateItemId(tool);
                toolInfo = tool.getType().name() + " [fst:" + fstId + "]";
            }

            String line = String.format("[%s] %s [%s] 使用 %s 放置了 %s 在 %s (%s)%s",
                ts, player.getName(), player.getUniqueId(), toolInfo, matName, world, coords, claimInfo);
            logArchive("playerChunk", line);

            boolean isDangerous = DANGEROUS.contains(matName);
            boolean noClaimPerm = claimInfo.isEmpty() && plugin.getClaimManager().getClaimAt(loc) != null && !isOp;
            if (isDangerous || noClaimPerm) {
                logOther("playerChunk", "§c[!] " + line);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("logBlockPlace 异常: " + e.getMessage());
        }
    }

    // ========== playerMove ==========

    private final Map<UUID, Location> lastMoveLoc = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();

    public void logPlayerMove(Player player, Location from, Location to) {
        if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        double dist = from.distance(to);
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();

        // Calculate speed (blocks/sec)
        Long lastT = lastMoveTime.get(uid);
        double speed = 0;
        if (lastT != null) {
            double dt = (now - lastT) / 1000.0;
            if (dt > 0) speed = dist / dt;
        }
        lastMoveTime.put(uid, now);
        lastMoveLoc.put(uid, to);

        if (speed <= 5) return;

        boolean hasElytra = player.getInventory().getChestplate() != null
            && player.getInventory().getChestplate().getType() == Material.ELYTRA;
        String elytraStr = hasElytra ? " [穿戴鞘翅]" : "";
        String world = to.getWorld().getName();
        String coords = to.getBlockX() + "," + to.getBlockY() + "," + to.getBlockZ();
        String line = String.format("[%s] %s [%s] 移动 %.1f块/秒 在 %s (%s)%s",
            now(), player.getName(), uid, speed, world, coords, elytraStr);
        logArchive("playerMove", line);

        if (speed > 20) {
            logOther("playerMove", "§c[!] " + line);
        }
    }

    // ========== playerJoin ==========

    private final Map<String, UUID> ipPlayerMap = new HashMap<>();

    public void logJoin(Player player) {
        String ts = now();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "?";
        UUID uid = player.getUniqueId();
        String name = player.getName();

        String line = String.format("[%s] %s [%s] 加入 IP=%s", ts, name, uid, ip);
        logArchive("playerJoin", line);

        // Check UUID/name mismatch
        String offlineName = Bukkit.getOfflinePlayer(uid).getName();
        if (offlineName != null && !offlineName.equals(name)) {
            logOther("playerJoin", "§c[!] UUID/名称不匹配: " + line);
        }

        // Check same IP different player
        UUID existing = ipPlayerMap.get(ip);
        if (existing != null && !existing.equals(uid)) {
            String otherName = Bukkit.getOfflinePlayer(existing).getName();
            logOther("playerJoin", "§c[!] 相同IP不同玩家: " + line + " (之前: " + otherName + " [" + existing + "])");
        }
        ipPlayerMap.put(ip, uid);
    }

    public void logQuit(Player player, boolean isKicked, String kickReason) {
        String ts = now();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "?";
        UUID uid = player.getUniqueId();
        String name = player.getName();
        String reason = isKicked ? "踢出: " + kickReason : "正常退出";

        String line = String.format("[%s] %s [%s] 断开 IP=%s 原因: %s", ts, name, uid, ip, reason);
        logArchive("playerJoin", line);

        if (isKicked) {
            logOther("playerJoin", "§c[!] " + line);
        }
    }

    // ========== playerItem ==========

    public void logItemPickup(Player player, ItemStack item) {
        String ts = now();
        Material type = item.getType();
        String matName = type.name();
        int amount = item.getAmount();
        UUID uid = player.getUniqueId();

        // Check special components/tags
        ItemMeta meta = item.getItemMeta();
        boolean hasEnchants = meta != null && meta.hasEnchants();
        boolean hasCustomTags = meta != null && !meta.getPersistentDataContainer().isEmpty();
        String extras = "";
        if (hasEnchants) extras += " [附魔]";
        if (hasCustomTags) extras += " [自定义标签]";

        String line = String.format("[%s] %s [%s] 拾取 %s x%d%s",
            ts, player.getName(), uid, matName, amount, extras);
        logArchive("playerItem", line);

        boolean isDangerous = DANGEROUS.contains(matName);
        boolean isValuable = VALUABLE.contains(matName);
        if (isDangerous || isValuable || hasEnchants || hasCustomTags) {
            logOther("playerItem", "§c[!] " + line);
        }
    }

    // ========== playerAttack ==========

    public void logAttack(Player damager, org.bukkit.entity.Entity target, double damage, double remainingHealth) {
        String ts = now();
        UUID duid = damager.getUniqueId();
        Location loc = damager.getLocation();
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
        String coords = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        UUID tuid = target.getUniqueId();

        String line = String.format("[%s] %s [%s] 在 %s (%s) 攻击了 %s [%s] 伤害=%.1f 目标剩余=%.1f",
            ts, damager.getName(), duid, world, coords, targetName, tuid, damage, remainingHealth);
        logArchive("playerAttack", line);

        // Track attack times for frequency check
        long now = System.currentTimeMillis();
        attackTimes.computeIfAbsent(duid, k -> new ArrayList<>()).add(now);
        // Clean old entries (>1s)
        attackTimes.get(duid).removeIf(t -> now - t > 1000);
        int attacksInSec = attackTimes.get(duid).size();

        // Track targets for multi-target check
        attackTargets.computeIfAbsent(duid, k -> new HashSet<>()).add(tuid);

        boolean highDmg = damage >= 10;
        boolean highFreq = attacksInSec >= 3;
        boolean multiTarget = attackTargets.get(duid).size() >= 3;

        if (highDmg || highFreq || multiTarget) {
            StringBuilder flags = new StringBuilder();
            if (highDmg) flags.append(" [高伤害]");
            if (highFreq) flags.append(" [高频 " + attacksInSec + "次/秒]");
            if (multiTarget) flags.append(" [多目标]");
            logOther("playerAttack", "§c[!] " + line + flags.toString());
        }
    }

    /** Reset multi-target counter per player periodically */
    public void resetAttackTracking() {
        attackTargets.clear();
    }

    // ========== playerDamageTaken ==========

    public void logPlayerDamageTaken(Player victim, double damage, EntityDamageEvent.DamageCause cause, double remainingHealth) {
        // Throttle: skip if we logged same player within 500ms
        UUID uid = victim.getUniqueId();
        long nowMs = System.currentTimeMillis();
        Long last = lastDamageLog.get(uid);
        if (last != null && nowMs - last < 500) return;
        lastDamageLog.put(uid, nowMs);

        try {
            Location loc = victim.getLocation();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
            String coords = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

            String line = String.format("[%s] %s [%s] 受到伤害 %.1f 来源: %s 剩余血量: %.1f 在 %s (%s)",
                now(), victim.getName(), uid, damage, cause.name(), remainingHealth, world, coords);
            logArchive("playerAttack", line);

            if (damage >= 10) {
                logOther("playerAttack", "§c[!] " + line + " [高伤害]");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("logPlayerDamageTaken 异常: " + e.getMessage());
        }
    }

    // ========== playerDeath ==========

    public void logPlayerDeath(Player player, String deathMessage, EntityDamageEvent.DamageCause cause) {
        String ts = now();
        Location loc = player.getLocation();
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
        String coords = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        UUID puid = player.getUniqueId();

        String line = String.format("[%s] %s [%s] 死亡 原因: %s 死亡消息: %s 在 %s (%s)",
            ts, player.getName(), puid, cause.name(), deathMessage, world, coords);
        logArchive("playerAttack", line);
        logOther("playerAttack", "§c[!] " + line);
    }

    // ========== Helpers ==========

    private static final Set<Material> CONTAINER_BLOCKS = Set.of(
        Material.CHEST, Material.TRAPPED_CHEST, Material.FURNACE, Material.BLAST_FURNACE,
        Material.SMOKER, Material.BARREL, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
        Material.BREWING_STAND, Material.COMPOSTER,
        Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX,
        Material.BLACK_SHULKER_BOX
    );

    private boolean isContainerBlock(Material mat) {
        return CONTAINER_BLOCKS.contains(mat);
    }

    private String getClaimInfo(Location loc) {
        ClaimRegion claim = plugin.getClaimManager().getClaimAt(loc);
        if (claim == null) return "";
        String ownerName = plugin.getServer().getOfflinePlayer(claim.getOwner()).getName();
        return " (在 " + (ownerName != null ? ownerName : "?") + " 的领地)";
    }
}
