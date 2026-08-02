package com.kat.coreessentials.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener implements Listener {

    private final CombatManager combat;
    private final TeleportDelayManager delays;
    private final long pvpSeconds;
    private final long pveSeconds;

    public CombatListener(CombatManager combat, TeleportDelayManager delays, long pvpSeconds, long pveSeconds) {
        this.combat = combat;
        this.delays = delays;
        this.pvpSeconds = pvpSeconds;
        this.pveSeconds = pveSeconds;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return;
        }

        Entity source = byEntity.getDamager();
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            source = shooter;
        }
        if (source.equals(victim)) {
            return;
        }

        if (source instanceof Player attacker) {
            // PvP: both sides get tagged, for the longer PvP duration.
            combat.tag(victim.getUniqueId(), pvpSeconds);
            combat.tag(attacker.getUniqueId(), pvpSeconds);
            cancelPending(victim, "you took damage!");
            cancelPending(attacker, "you entered combat!");
        } else if (source instanceof Mob) {
            // PvE: only the player taking the hit is tagged, for the shorter PvE duration.
            combat.tag(victim.getUniqueId(), pveSeconds);
            cancelPending(victim, "you took damage!");
        }
    }

    private void cancelPending(Player player, String reason) {
        if (delays.cancel(player.getUniqueId())) {
            player.sendMessage(Component.text("Teleport cancelled - " + reason, NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combat.isInCombat(player.getUniqueId()) && player.getHealth() > 0) {
            // Combat logging: kill them on the spot before they fully disconnect.
            player.setHealth(0.0);
        }
        combat.clear(player.getUniqueId());
        delays.clear(player.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        combat.clear(event.getEntity().getUniqueId());
        delays.clear(event.getEntity().getUniqueId());
    }
}
