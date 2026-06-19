package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.ClaimRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ClaimParticleManager {

    private final FunstartPlugin plugin;
    private int taskId = -1;

    public ClaimParticleManager(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 15L).getTaskId();
    }

    public void stop() {
        if (taskId >= 0) Bukkit.getScheduler().cancelTask(taskId);
    }

    private void tick() {
        List<ClaimRegion> claims = plugin.getClaimManager().getAllClaims();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ClaimRegion claim : claims) {
                if (!player.getWorld().getName().equals(claim.getWorldName())) continue;
                double cx = (claim.getX1() + claim.getX2()) / 2.0;
                double cz = (claim.getZ1() + claim.getZ2()) / 2.0;
                if (player.getLocation().distanceSquared(new Location(player.getWorld(), cx, player.getY(), cz)) > 2500.0) continue;

                boolean isOwner = claim.getOwner().equals(plugin.getEffectiveUuid(player));
                Particle particle = isOwner ? Particle.HAPPY_VILLAGER : Particle.WHITE_SMOKE;

                showClaimBorders(player, claim, particle);
            }

            // Claim session preview particles
            UUID uid = player.getUniqueId();
            if (plugin.getClaimManager().hasActiveSession(uid)) {
                ClaimManager.ClaimSessionData csd = plugin.getClaimManager().getSessionData(uid);
                if (csd != null && csd.firstSet && csd.claimWorld != null && csd.breakWorld != null
                    && csd.claimWorld.equals(csd.breakWorld)) {
                    showSessionPreview(player, csd);
                }
            }
        }
    }

    private void showClaimBorders(Player player, ClaimRegion claim, Particle particle) {
        int x1 = Math.min(claim.getX1(), claim.getX2());
        int x2 = Math.max(claim.getX1(), claim.getX2());
        int z1 = Math.min(claim.getZ1(), claim.getZ2());
        int z2 = Math.max(claim.getZ1(), claim.getZ2());
        int y1 = Math.min(claim.getY1(), claim.getY2());
        int y2 = Math.max(claim.getY1(), claim.getY2());

        // Bottom edges (y1)
        showRectangleEdges(player, x1, z1, x2, z2, y1, particle);
        // Top edges (y2)
        showRectangleEdges(player, x1, z1, x2, z2, y2, particle);
    }

    private void showRectangleEdges(Player player, int x1, int z1, int x2, int z2, int y, Particle particle) {
        for (int x = x1; x <= x2; x += 2) {
            player.spawnParticle(particle, x + 0.5, y + 0.5, z1 + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, x + 0.5, y + 0.5, z2 + 0.5, 1, 0, 0, 0, 0);
        }
        for (int z = z1 + 1; z < z2; z += 2) {
            player.spawnParticle(particle, x1 + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, x2 + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0);
        }
    }

    private void showSessionPreview(Player player, ClaimManager.ClaimSessionData csd) {
        int x1 = Math.min(csd.x1, csd.x2);
        int x2 = Math.max(csd.x1, csd.x2);
        int y1 = Math.min(csd.y1, csd.y2);
        int y2 = Math.max(csd.y1, csd.y2);
        int z1 = Math.min(csd.z1, csd.z2);
        int z2 = Math.max(csd.z1, csd.z2);

        // Show all 12 edges of the 3D bounding box
        // Edges along X at bottom and top
        for (int x = x1; x <= x2; x += 2) {
            player.spawnParticle(Particle.WHITE_SMOKE, x + 0.5, y1 + 0.5, z1 + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x + 0.5, y1 + 0.5, z2 + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x + 0.5, y2 + 0.5, z1 + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x + 0.5, y2 + 0.5, z2 + 0.5, 1, 0, 0, 0, 0);
        }
        // Edges along Z at bottom and top
        for (int z = z1 + 1; z < z2; z += 2) {
            player.spawnParticle(Particle.WHITE_SMOKE, x1 + 0.5, y1 + 0.5, z + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x2 + 0.5, y1 + 0.5, z + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x1 + 0.5, y2 + 0.5, z + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x2 + 0.5, y2 + 0.5, z + 0.5, 1, 0, 0, 0, 0);
        }
        // Vertical edges (along Y) at the 4 corners
        for (int y = y1 + 1; y < y2; y += 2) {
            player.spawnParticle(Particle.WHITE_SMOKE, x1 + 0.5, y + 0.5, z1 + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x1 + 0.5, y + 0.5, z2 + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x2 + 0.5, y + 0.5, z1 + 0.5, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.WHITE_SMOKE, x2 + 0.5, y + 0.5, z2 + 0.5, 1, 0, 0, 0, 0);
        }
    }
}
