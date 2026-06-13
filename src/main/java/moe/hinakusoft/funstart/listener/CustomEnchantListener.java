package moe.hinakusoft.funstart.listener;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.manager.FSTActionBar;
import moe.hinakusoft.funstart.model.CustomEnchantment;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CustomEnchantListener implements Listener {

    private final FunstartPlugin plugin;
    private final FSTActionBar actionBar;
    private final Random random = new Random();

    private static final Set<Material> ORES = Set.of(
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE
    );

    private final Map<UUID, Long> lastFrostTick = new HashMap<>();
    private final Map<UUID, Long> lastLeafTick = new HashMap<>();
    private final Map<UUID, Long> lastExplosiveThrow = new ConcurrentHashMap<>();
    private final Set<UUID> shikiEquipped = new HashSet<>();

    public CustomEnchantListener(FunstartPlugin plugin) {
        this.plugin = plugin;
        this.actionBar = plugin.getActionBar();

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                applyFrostTrail(player);
                applyLeafHidden(player);
                applyShikiHold(player);
            }
        }, 0L, 5L);
    }

    private int getHighestLevel(Player player, CustomEnchantment ce) {
        int level = 0;
        level = Math.max(level, EnchantGuiListener.getCustomLevel(player.getInventory().getItemInMainHand(), plugin, ce));
        level = Math.max(level, EnchantGuiListener.getCustomLevel(player.getInventory().getItemInOffHand(), plugin, ce));
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            level = Math.max(level, EnchantGuiListener.getCustomLevel(armor, plugin, ce));
        }
        return level;
    }

    // ========== SHIKI_NATSUME ==========

    private List<String> getShikiMessages(String category) {
        return plugin.getConfig().getStringList("shiki-natsume.messages." + category);
    }

    private void sendShikiActionBar(Player player, String category) {
        List<String> msgs = getShikiMessages(category);
        if (msgs.isEmpty()) return;
        String msg = msgs.get(random.nextInt(msgs.size()));
        actionBar.add(player, "§c§l四季 夏目： §r§c§o" + msg);
    }

    private boolean hasShikiInHand(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() != org.bukkit.Material.DIAMOND_AXE) return false;
        return EnchantGuiListener.getCustomLevel(main, plugin, CustomEnchantment.SHIKI_NATSUME) > 0;
    }

    private void applyShikiHold(Player player) {
        boolean has = hasShikiInHand(player);
        UUID uid = player.getUniqueId();
        boolean was = shikiEquipped.contains(uid);
        if (has && !was) {
            shikiEquipped.add(uid);
            sendShikiActionBar(player, "hold");
        } else if (!has && was) {
            shikiEquipped.remove(uid);
        }
    }

    // ========== MINER_AGILITY ==========

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (hasShikiInHand(player) && random.nextInt(100) < 16) {
            sendShikiActionBar(player, "break");
        }

        int level = getHighestLevel(player, CustomEnchantment.MINER_AGILITY);
        if (level <= 0) return;

        Block block = event.getBlock();
        Material type = block.getType();
        if (!ORES.contains(type)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) return;

        if (random.nextInt(100) < Math.min(level * 8, 80)) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                new ItemStack(type, 1));
            actionBar.add(player, "§5[矿工之敏] §a双倍掉落!");
        }
    }

    // ========== LIFE_STEAL ==========

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (hasShikiInHand(killer) && random.nextInt(100) < 75) {
            sendShikiActionBar(killer, "kill");
        }

        int level = getHighestLevel(killer, CustomEnchantment.LIFE_STEAL);
        if (level <= 0) return;

        double heal = Math.min(level * 2.0, killer.getMaxHealth() - killer.getHealth());
        if (heal <= 0) return;
        killer.setHealth(killer.getHealth() + heal);
        actionBar.add(killer, String.format("§5[生命恢复] §a恢复 §c❤ %.0f", heal));
    }

    // ========== FROST_TRAIL ==========

    private void applyFrostTrail(Player player) {
        int level = getHighestLevel(player, CustomEnchantment.FROST_TRAIL);
        if (level <= 0) return;

        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (lastFrostTick.containsKey(uid) && now - lastFrostTick.get(uid) < 500) return;
        lastFrostTick.put(uid, now);

        if (!player.isOnGround()) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        int radius = 2 + (int)((level - 1) * 2.0 / 3);
        int duration = 60 + (int)((level - 1) * 100.0 / 9);
        int amp = level >= 6 ? 1 : 0;

        world.spawnParticle(Particle.SNOWFLAKE, loc.add(0, 1, 0), 8, 1.5, 0.5, 1.5, 0);

        for (LivingEntity entity : world.getNearbyLivingEntities(loc, radius, 1.5, radius)) {
            if (entity == player) continue;
            if (entity instanceof Player) continue;
            if (entity instanceof Villager || entity instanceof Cat || entity instanceof Wolf) continue;
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amp, true, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0, true, false));
        }
    }

    // ========== ENDER_MOVE ==========

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (hasShikiInHand(player) && random.nextInt(100) < 66) {
            sendShikiActionBar(player, "damage");
        }

        int level = getHighestLevel(player, CustomEnchantment.ENDER_MOVE);
        if (level <= 0) return;

        if (player.hasPotionEffect(PotionEffectType.RESISTANCE)) return;

        int duration = level * 10;
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, true, false));
        actionBar.add(player, String.format("§5[龟壳移动] §a抗性提升 §7(%.1fs)", duration / 20.0));
    }

    // ========== EXPLOSIVE_THROW ==========

    private long getExplosiveCooldown(int level) {
        long cd = 1950L - (level - 1) * 65L;
        return Math.max(cd, 1690L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) return;

        int level = getHighestLevel(player, CustomEnchantment.EXPLOSIVE_THROW);
        if (level <= 0) return;

        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUse = lastExplosiveThrow.get(uid);
        long cd = getExplosiveCooldown(level);
        if (lastUse != null && now - lastUse < cd) {
            actionBar.add(player, "§5[爆裂投掷] §c冷却中 (" + ((cd - (now - lastUse)) / 1000 + 1) + "s)");
            return;
        }
        lastExplosiveThrow.put(uid, now);

        Location hitLoc = projectile.getLocation();
        float power = (0.3f + (level - 1) * 0.3f) * 1.35f;

        // Visual explosion
        hitLoc.getWorld().createExplosion(hitLoc, power, false, false, player);

        // Manual damage with custom rules
        double radius = power * 2.5;
        double maxDamage = (6.0 + level * 2.0) * 1.35;

        for (Entity entity : hitLoc.getWorld().getNearbyEntities(hitLoc, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(player)) continue;
            if (entity instanceof Villager || entity instanceof Cat || entity instanceof Wolf) continue;

            double distance = entity.getLocation().distance(hitLoc);
            double damage = maxDamage * Math.max(0, 1 - distance / radius);
            if (damage < 1) continue;
            if (entity instanceof Player) damage *= 0.65;

            living.damage(damage, player);
        }

        actionBar.add(player, "§5[爆裂投掷] §a爆炸!");
    }

    // ========== LEAF_HIDDEN ==========

    private static final Material[] LEAF_TYPES = {
        Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES,
        Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
        Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.PALE_OAK_LEAVES,
        Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
    };

    private void applyLeafHidden(Player player) {
        int level = getHighestLevel(player, CustomEnchantment.LEAF_HIDDEN);
        if (level <= 0) return;

        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (lastLeafTick.containsKey(uid) && now - lastLeafTick.get(uid) < 500) return;
        lastLeafTick.put(uid, now);

        Location loc = player.getLocation();
        Block below = loc.getBlock().getRelative(0, -1, 0);
        boolean onLeaves = false;
        for (Material leaf : LEAF_TYPES) {
            if (below.getType() == leaf) { onLeaves = true; break; }
        }

        // Check if any leaves within level-radius (horizontal, y: -1~+2)
        int r = Math.min(level, 8);
        boolean nearLeaves = onLeaves; // already on leaves counts
        if (!nearLeaves) {
            Block origin = loc.getBlock();
            for (int x = -r; x <= r && !nearLeaves; x++) {
                for (int z = -r; z <= r && !nearLeaves; z++) {
                    if (x*x + z*z > r*r) continue;
                    for (int y = -1; y <= 2 && !nearLeaves; y++) {
                        Block b = origin.getRelative(x, y, z);
                        for (Material leaf : LEAF_TYPES) {
                            if (b.getType() == leaf) { nearLeaves = true; break; }
                        }
                    }
                }
            }
        }

        if (!nearLeaves) return;

        // All effects: 5s (100 ticks), level I (amp 0)
        if (onLeaves) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, true, false));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, false));

        actionBar.add(player, "§5[叶隐] §a" + (onLeaves ? "隐身加速回复" : "加速回复"));
    }

    // ========== TIME_SLOW ==========

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        if (hasShikiInHand(player) && random.nextInt(100) < 22) {
            sendShikiActionBar(player, "attack");
        }

        if (!(event.getEntity() instanceof LivingEntity target)) return;

        int level = getHighestLevel(player, CustomEnchantment.TIME_SLOW);
        if (level <= 0) return;

        if (random.nextInt(100) < level * 20) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60 + level * 20, level - 1, true, false));
            actionBar.add(player, "§5[时间减缓] §a减速目标");
        }
    }
}
