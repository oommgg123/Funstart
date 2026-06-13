package moe.hinakusoft.funstart.listener;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.manager.LogManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LogListener implements Listener {

    private final FunstartPlugin plugin;
    private final LogManager logManager;

    public LogListener(FunstartPlugin plugin) {
        this.plugin = plugin;
        this.logManager = plugin.getLogManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        logManager.logJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        logManager.logQuit(event.getPlayer(), false, "");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        logManager.logQuit(event.getPlayer(), true, event.getReason());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()) return;
        logManager.logPlayerMove(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        logManager.logItemPickup(player, event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        double remaining = Math.max(0, target.getHealth() - event.getFinalDamage());
        logManager.logAttack(damager, target, event.getFinalDamage(), remaining);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        double remaining = Math.max(0, victim.getHealth() - event.getFinalDamage());
        logManager.logPlayerDamageTaken(victim, event.getFinalDamage(), event.getCause(), remaining);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String deathMsg = event.getDeathMessage() != null ? event.getDeathMessage() : "?";
        org.bukkit.event.entity.EntityDamageEvent lastDamage = player.getLastDamageCause();
        EntityDamageEvent.DamageCause cause = lastDamage != null ? lastDamage.getCause() : EntityDamageEvent.DamageCause.CUSTOM;
        logManager.logPlayerDeath(player, deathMsg, cause);
        logManager.resetAttackTracking();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamagedByAnything(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        // Already handled in onPlayerDamaged for entity-on-player damage
        if (event instanceof EntityDamageByEntityEvent) return;
        double remaining = Math.max(0, victim.getHealth() - event.getFinalDamage());
        logManager.logPlayerDamageTaken(victim, event.getFinalDamage(), event.getCause(), remaining);
    }
}
