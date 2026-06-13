package moe.hinakusoft.funstart.listener;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.ClaimRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClaimListener implements Listener {

    private final FunstartPlugin plugin;
    private final Set<UUID> inSpawnZone = new HashSet<>();

    public ClaimListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        // Track block breaks for claim creation sessions (always, even if cancelled)
        plugin.getClaimManager().setPendingBreak(player, loc);

        if (event.isCancelled()) return;
        if (player.isOp()) return;

        // Spawn protection
        if (plugin.getClaimManager().isInSpawnProtection(loc)) {
            event.setCancelled(true);
            player.sendMessage("§c出生点保护区内无法破坏方块");
            return;
        }

        // Claim protection
        ClaimRegion claim = plugin.getClaimManager().getClaimAt(loc);
        if (claim != null) {
            UUID uid = player.getUniqueId();
            if (!claim.getOwner().equals(uid) && !claim.getTrustedPlayers().contains(uid)) {
                event.setCancelled(true);
                player.sendMessage("§c你没有权限在此区域破坏方块");
            }
        }
        plugin.getLogManager().logBlockBreak(player, event.getBlock(), player.isOp(), player.getInventory().getItemInMainHand());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        if (event.isCancelled()) return;
        if (player.isOp()) return;

        if (plugin.getClaimManager().isInSpawnProtection(loc)) {
            event.setCancelled(true);
            player.sendMessage("§c出生点保护区内无法放置方块");
            return;
        }

        ClaimRegion claim = plugin.getClaimManager().getClaimAt(loc);
        if (claim != null) {
            UUID uid = player.getUniqueId();
            if (!claim.getOwner().equals(uid) && !claim.getTrustedPlayers().contains(uid)) {
                event.setCancelled(true);
                player.sendMessage("§c你没有权限在此区域放置方块");
            }
        }
        plugin.getLogManager().logBlockPlace(player, event.getBlock(), player.isOp(), player.getInventory().getItemInMainHand());
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        // Per-claim TNT/creeper settings
        if (event.getEntity() instanceof TNTPrimed) {
            event.blockList().removeIf(block -> {
                Location loc = block.getLocation();
                if (plugin.getClaimManager().isInSpawnProtection(loc)) return true;
                ClaimRegion claim = plugin.getClaimManager().getClaimAt(loc);
                return claim != null && !claim.isAllowTnt();
            });
        } else if (event.getEntity() instanceof Creeper) {
            event.blockList().removeIf(block -> {
                Location loc = block.getLocation();
                if (plugin.getClaimManager().isInSpawnProtection(loc)) return true;
                ClaimRegion claim = plugin.getClaimManager().getClaimAt(loc);
                return claim != null && !claim.isAllowCreeper();
            });
        } else {
            // Other explosions: always block in claims and spawn
            event.blockList().removeIf(block -> {
                Location loc = block.getLocation();
                if (plugin.getClaimManager().isInSpawnProtection(loc)) return true;
                return plugin.getClaimManager().getClaimAt(loc) != null;
            });
        }
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent event) {
        if (event.isCancelled()) return;
        if (event.getSource().getType() != Material.FIRE) return;
        Location loc = event.getBlock().getLocation();
        if (plugin.getClaimManager().getClaimAt(loc) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        Location to = event.getTo();
        if (to == null) return;
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();

        boolean nowInSpawn = plugin.getClaimManager().isInSpawnProtection(to);
        boolean wasInSpawn = inSpawnZone.contains(uid);

        if (nowInSpawn && !wasInSpawn) {
            inSpawnZone.add(uid);
            player.sendMessage("§e[Funstart] §6你已进入出生点保护区 (§e±" + plugin.getClaimManager().getSpawnRadius() + "§6)");
        } else if (!nowInSpawn && wasInSpawn) {
            inSpawnZone.remove(uid);
            player.sendMessage("§e[Funstart] §a你已离开出生点保护区");
        }

        // Claim entry/exit
        ClaimRegion claim = plugin.getClaimManager().getClaimAt(to);
        ClaimRegion prevClaim = plugin.getClaimManager().getClaimAt(event.getFrom());

        if (claim != null && !claim.equals(prevClaim) && !claim.getOwner().equals(uid)) {
            String ownerName = plugin.getServer().getOfflinePlayer(claim.getOwner()).getName();
            player.sendMessage("§e[Funstart] §b" + (ownerName != null ? ownerName : "未知") + " §7的领地");
        }
    }

    private static final Set<Material> PROTECTED_INTERACT = Set.of(
        Material.FURNACE, Material.CHEST, Material.TRAPPED_CHEST, Material.BLAST_FURNACE,
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.isOp()) return;
        Block block = event.getClickedBlock();
        if (block == null || !PROTECTED_INTERACT.contains(block.getType())) return;
        Location loc = block.getLocation();
        ClaimRegion claim = plugin.getClaimManager().getClaimAt(loc);
        if (claim == null) return;
        UUID uid = player.getUniqueId();
        if (!claim.getOwner().equals(uid) && !claim.getTrustedPlayers().contains(uid)) {
            event.setCancelled(true);
            player.sendMessage("§c你没有权限使用该容器/装置");
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getRightClicked() instanceof ArmorStand)) return;
        Player player = event.getPlayer();
        if (player.isOp()) return;
        Location loc = event.getRightClicked().getLocation();
        ClaimRegion claim = plugin.getClaimManager().getClaimAt(loc);
        if (claim == null) return;
        UUID uid = player.getUniqueId();
        if (!claim.getOwner().equals(uid) && !claim.getTrustedPlayers().contains(uid)) {
            event.setCancelled(true);
            player.sendMessage("§c你没有权限使用该盔甲架");
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        Location loc = event.getLocation();
        if (plugin.getClaimManager().isInSpawnProtection(loc)) {
            event.setCancelled(true);
        }
    }

    public Set<UUID> getInSpawnZone() { return inSpawnZone; }
}
