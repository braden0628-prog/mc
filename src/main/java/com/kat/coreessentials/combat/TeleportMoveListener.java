package com.kat.coreessentials.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeleportMoveListener implements Listener {

    private final TeleportDelayManager delays;

    public TeleportMoveListener(TeleportDelayManager delays) {
        this.delays = delays;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        // Comparing exact X/Y/Z doesn't work reliably - the client sends tiny
        // sub-block position jitter even when just turning the camera in place,
        // which would falsely count as "moved". A small distance threshold
        // filters that out while still catching any real step/jump/strafe.
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dy = event.getTo().getY() - event.getFrom().getY();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared < 0.01) {
            return;
        }

        Player player = event.getPlayer();
        if (delays.cancel(player.getUniqueId())) {
            player.sendActionBar(Component.text("Teleport cancelled - you moved!", NamedTextColor.RED));
        }
    }
}
