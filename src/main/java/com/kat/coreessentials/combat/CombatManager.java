package com.kat.coreessentials.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how long each player remains "in combat" after taking damage from a
 * mob or another player. While tagged, teleporting (and accepting teleport
 * requests) is blocked, and disconnecting kills the player on the spot.
 */
public class CombatManager {

    private final Map<UUID, Long> combatUntilMillis = new ConcurrentHashMap<>();
    private final long tagDurationMillis;

    public CombatManager(long tagDurationSeconds) {
        this.tagDurationMillis = tagDurationSeconds * 1000L;
    }

    public void tag(UUID player) {
        combatUntilMillis.put(player, System.currentTimeMillis() + tagDurationMillis);
    }

    public boolean isInCombat(UUID player) {
        Long until = combatUntilMillis.get(player);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            combatUntilMillis.remove(player);
            return false;
        }
        return true;
    }

    /** Seconds left on the combat tag, 0 if not in combat. */
    public long secondsRemaining(UUID player) {
        Long until = combatUntilMillis.get(player);
        if (until == null) {
            return 0;
        }
        long remaining = (until - System.currentTimeMillis() + 999) / 1000L;
        return Math.max(remaining, 0);
    }

    public void clear(UUID player) {
        combatUntilMillis.remove(player);
    }
}
