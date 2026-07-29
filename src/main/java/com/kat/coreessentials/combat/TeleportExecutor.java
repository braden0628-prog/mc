package com.kat.coreessentials.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeleportExecutor {

    /** Overwritten from config.yml (teleport.delay-seconds) on plugin enable. */
    public static int delaySeconds = 5;

    private TeleportExecutor() {
    }

    /**
     * Attempts to start a delayed teleport for a player. Rejects outright if
     * they're in combat or already mid-teleport; otherwise counts down and
     * teleports them, unless cancelled (see TeleportDelayManager / CombatListener).
     *
     * @return true if the teleport was accepted and scheduled, false if rejected.
     */
    public static boolean request(JavaPlugin plugin, CombatManager combat, TeleportDelayManager delays,
                                   Player player, Location destination, String successMessage) {
        if (combat.isInCombat(player.getUniqueId())) {
            player.sendMessage(Component.text(
                "You can't teleport while in combat! (" + combat.secondsRemaining(player.getUniqueId()) + "s left)",
                NamedTextColor.RED
            ));
            return false;
        }
        if (delays.hasPending(player.getUniqueId())) {
            player.sendMessage(Component.text("You're already teleporting.", NamedTextColor.RED));
            return false;
        }

        player.sendMessage(Component.text(
            "Teleporting in " + delaySeconds + " second" + (delaySeconds == 1 ? "" : "s") + "... don't move or take damage.",
            NamedTextColor.YELLOW
        ));

        var task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            delays.clear(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            player.teleportAsync(destination);
            if (successMessage != null) {
                player.sendMessage(Component.text(successMessage, NamedTextColor.GREEN));
            }
        }, delaySeconds * 20L);

        delays.setPending(player.getUniqueId(), task);
        return true;
    }
}
