package com.kat.coreessentials.combat;

import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the currently-pending delayed teleport (if any) for each player, so
 * it can be cancelled - e.g. if they take damage during the countdown.
 */
public class TeleportDelayManager {

    private final Map<UUID, BukkitTask> pending = new ConcurrentHashMap<>();

    public boolean hasPending(UUID player) {
        return pending.containsKey(player);
    }

    public void setPending(UUID player, BukkitTask task) {
        pending.put(player, task);
    }

    /** Cancels a pending teleport. Returns true if one was actually cancelled. */
    public boolean cancel(UUID player) {
        BukkitTask task = pending.remove(player);
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    public void clear(UUID player) {
        BukkitTask task = pending.remove(player);
        if (task != null) {
            task.cancel();
        }
    }
}
