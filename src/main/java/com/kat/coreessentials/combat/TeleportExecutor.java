package com.kat.coreessentials.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Supplier;

public final class TeleportExecutor {

    /** Overwritten from config.yml (teleport.delay-seconds) on plugin enable. */
    public static int delaySeconds = 5;

    private TeleportExecutor() {
    }

    /**
     * Attempts to start a delayed teleport for a player. Rejects outright if
     * they're in combat or already mid-teleport; otherwise runs a per-second
     * countdown (action bar text + tick sound) and teleports them at zero,
     * unless cancelled in the meantime (see TeleportDelayManager / CombatListener).
     * The destination is resolved fresh from destinationSupplier at the exact
     * moment the teleport actually happens - not captured up front - so
     * following a moving player (e.g. accepting a /tpr) tracks their live
     * position rather than wherever they were when the request was accepted.
     * If the supplier returns null (e.g. the target went offline mid-countdown),
     * the teleport is cancelled gracefully with a message instead of erroring.
     *
     * @return true if the teleport was accepted and scheduled, false if rejected.
     */
    public static boolean request(JavaPlugin plugin, CombatManager combat, TeleportDelayManager delays,
                                   Player player, Supplier<Location> destinationSupplier, String successMessage) {
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

        BukkitRunnable countdown = new BukkitRunnable() {
            int remaining = delaySeconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    delays.clear(player.getUniqueId());
                    cancel();
                    return;
                }

                if (remaining <= 0) {
                    delays.clear(player.getUniqueId());
                    Location destination = destinationSupplier.get();
                    if (destination == null) {
                        player.sendMessage(Component.text("Teleport cancelled - destination is no longer available.", NamedTextColor.RED));
                        cancel();
                        return;
                    }
                    player.teleportAsync(destination);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    if (successMessage != null) {
                        player.sendActionBar(Component.text(successMessage, NamedTextColor.GREEN));
                    }
                    cancel();
                    return;
                }

                player.sendActionBar(Component.text(
                    "Teleporting in " + remaining + "s...",
                    NamedTextColor.YELLOW
                ));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                remaining--;
            }
        };

        var task = countdown.runTaskTimer(plugin, 0L, 20L);
        delays.setPending(player.getUniqueId(), task);
        return true;
    }

    /** Convenience overload for a fixed destination that doesn't change over time (homes, spawn, RTP, etc). */
    public static boolean request(JavaPlugin plugin, CombatManager combat, TeleportDelayManager delays,
                                   Player player, Location destination, String successMessage) {
        return request(plugin, combat, delays, player, () -> destination, successMessage);
    }
}
