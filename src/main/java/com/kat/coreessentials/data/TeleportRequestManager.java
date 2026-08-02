package com.kat.coreessentials.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending teleport requests - both /tpr ("come to me" style, where
 * the requester teleports to the target once accepted) and /tph ("teleport
 * here" style, where the target teleports to the requester once accepted).
 * Keyed by the TARGET's UUID (the player who received the request); only
 * the most recent incoming request per target is kept.
 */
public class TeleportRequestManager {

    /** targetMovesToRequester: true for /tph, false for /tpr. */
    public record Request(UUID requester, boolean targetMovesToRequester, long expiresAtMillis) {
    }

    private final Map<UUID, Request> pending = new ConcurrentHashMap<>();
    private final long expireMillis;

    public TeleportRequestManager(long expireSeconds) {
        this.expireMillis = expireSeconds * 1000L;
    }

    public void addRequest(UUID target, UUID requester, boolean targetMovesToRequester) {
        pending.put(target, new Request(requester, targetMovesToRequester, System.currentTimeMillis() + expireMillis));
    }

    /** Returns the pending request for this target if valid and unexpired, otherwise null. */
    public Request getValidRequest(UUID target) {
        Request request = pending.get(target);
        if (request == null) {
            return null;
        }
        if (System.currentTimeMillis() > request.expiresAtMillis()) {
            pending.remove(target);
            return null;
        }
        return request;
    }

    public void clear(UUID target) {
        pending.remove(target);
    }

    public void clearAllFrom(UUID requester) {
        pending.entrySet().removeIf(entry -> entry.getValue().requester().equals(requester));
    }
}
