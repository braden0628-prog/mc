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

    public CombatListener(CombatManager combat, TeleportDelayManager delays) {
        this.combat = combat;
        this.delays = delays;
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

        // Combat is tagged for damage dealt by another player, or by a mob.
        boolean isCombatSource = source instanceof Player || source instanceof Mob;
        if (!isCombatSource || source.equals(victim)) {
            return;
        }

        combat.tag(victim.getUniqueId());

        if (delays.cancel(victim.getUniqueId())) {
            victim.sendMessage(Component.text("Teleport cancelled - you took damage!", NamedTextColor.RED));
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
