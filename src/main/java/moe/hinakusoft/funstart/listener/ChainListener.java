package moe.hinakusoft.funstart.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.manager.BlockUtils;
import moe.hinakusoft.funstart.model.ClaimRegion;
import moe.hinakusoft.funstart.model.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class ChainListener implements Listener {

    private static final Set<Material> CROPS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES,
        Material.BEETROOTS, Material.NETHER_WART
    );

    private final FunstartPlugin plugin;

    public ChainListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        PlayerData data = this.plugin.getPlayerDataManager().getPlayerData(player);
        if (!data.isChainEnabled()) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isValidChainTool(tool)) return;

        Block origin = event.getBlock();
        Material type = origin.getType();
        if (type.isAir()) return;

        if (isCropBlock(origin)) return;

        int maxBlocks = data.getMaxChainBlocks();

        Set<Block> toBreak = BlockUtils.floodFill(origin, b -> b.getType() == type, maxBlocks);
        if (toBreak.size() <= 1) return;

        int estimatedNonOre = 0;
        for (Block b : toBreak) {
            if (!isOreBlock(b)) estimatedNonOre++;
        }
        double estimatedCost = estimatedNonOre / 16.0;
        if (estimatedCost > 0 && data.getPoints() < estimatedCost) {
            player.sendMessage("§c点数不足，无法连锁!");
            return;
        }

        event.setCancelled(true);

        List<Block> actuallyBroken = new ArrayList<>();

        for (Block block : toBreak) {
            // Skip protected blocks
            if (plugin.getClaimManager().isInSpawnProtection(block.getLocation())) continue;
            ClaimRegion claim = plugin.getClaimManager().getClaimAt(block.getLocation());
            if (claim != null && !claim.getOwner().equals(player.getUniqueId())
                && !claim.getTrustedPlayers().contains(player.getUniqueId())) continue;

            tool = player.getInventory().getItemInMainHand();
            if (tool == null || tool.getType().isAir() || !isValidChainTool(tool)) {
                if (!actuallyBroken.isEmpty()) {
                    player.sendMessage("§c工具已损坏，连锁停止");
                }
                break;
            }
            actuallyBroken.add(block);
            block.breakNaturally(tool);
            tool.damage(1, player);
        }

        int actualNonOre = 0;
        for (Block b : actuallyBroken) {
            if (!isOreBlock(b)) actualNonOre++;
        }
        double actualCost = actualNonOre / 16.0;

        Location originLoc = origin.getLocation().add(0.5, 0.5, 0.5);
        for (Block b : actuallyBroken) {
            for (Item item : b.getLocation().getNearbyEntitiesByType(Item.class, 1.5)) {
                item.teleport(originLoc);
            }
        }

        if (actualCost > 0) {
            data.deductPoints(actualCost);
            player.sendMessage("§e[Funstart] §a连锁挖掘 §b" + actuallyBroken.size() + " §a个方块, 消耗 §e" + PlayerData.fmt(actualCost) + " §a点, 剩余 §e" + PlayerData.fmt(data.getPoints()) + " §a点");
        } else {
            player.sendMessage("§e[Funstart] §a连锁挖掘 §b" + actuallyBroken.size() + " §a个方块");
        }
    }

    private boolean isValidChainTool(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Material mat = item.getType();
        String name = mat.name();
        return name.contains("PICKAXE") || name.contains("SHOVEL") ||
               name.contains("HOE") || name.contains("SWORD") ||
               name.contains("AXE") || name.contains("SHEARS");
    }

    private boolean isOreBlock(Block block) {
        Material type = block.getType();
        if (type == Material.ANCIENT_DEBRIS) return true;
        return type.name().endsWith("_ORE");
    }

    private boolean isCropBlock(Block block) {
        Material type = block.getType();
        if (!CROPS.contains(type)) return false;
        BlockData data = block.getBlockData();
        return data instanceof Ageable;
    }
}
