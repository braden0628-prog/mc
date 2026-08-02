package com.kat.coreessentials.combat;

import com.kat.coreessentials.data.TeamManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Blocks damage between players on the same team - melee, arrows, tridents,
 * thrown/splash potions (unwrapped via the projectile's shooter). Runs at
 * LOWEST priority so it cancels the event before CombatListener sees it,
 * meaning a blocked friendly-fire hit never tags either player "in combat"
 * either. Does not cover indirect sources like TNT or other explosions.
 */
public class TeamFriendlyFireListener implements Listener {

    private final TeamManager teams;

    public TeamFriendlyFireListener(TeamManager teams) {
        this.teams = teams;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Entity source = event.getDamager();
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            source = shooter;
        }

        if (!(source instanceof Player attacker) || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        String attackerTeam = teams.getTeamOf(attacker.getUniqueId());
        if (attackerTeam == null) {
            return;
        }

        if (attackerTeam.equals(teams.getTeamOf(victim.getUniqueId()))) {
            event.setCancelled(true);
        }
    }
}
