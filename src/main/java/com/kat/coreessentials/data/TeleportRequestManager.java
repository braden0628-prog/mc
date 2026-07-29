package com.kat.coreessentials.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending /tpr requests. Keyed by the TARGET's UUID (the player who
 * received the request), value is the REQUESTER's UUID. Only the most
 * recent incoming request per target is kept.
 */
public class TeleportRequestManager {

    public record Request(UUID requester, long expiresAtMillis) {
    }

    private final Map<UUID, Request> pending = new ConcurrentHashMap<>();
    private final long expireMillis;

    public TeleportRequestManager(long expireSeconds) {
        this.expireMillis = expireSeconds * 1000L;
    }

    public void addRequest(UUID target, UUID requester) {
        pending.put(target, new Request(requester, System.currentTimeMillis() + expireMillis));
    }

    /** Returns the requester's UUID if there's a valid, unexpired request for this target. */
    public UUID getValidRequester(UUID target) {
        Request request = pending.get(target);
        if (request == null) {
            return null;
        }
        if (System.currentTimeMillis() > request.expiresAtMillis()) {
            pending.remove(target);
            return null;
        }
        return request.requester();
    }

    public void clear(UUID target) {
        pending.remove(target);
    }

    public void clearAllFrom(UUID requester) {
        pending.entrySet().removeIf(entry -> entry.getValue().requester().equals(requester));
    }
}
