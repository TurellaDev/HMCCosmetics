package com.hibiscusmc.hmccosmetics.cache;

import com.hibiscusmc.hmccosmetics.messaging.message.CosmeticUpdateMessage;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CosmeticCache {

    private static final Map<UUID, Map<String, CosmeticUpdateMessage>> PENDING_UPDATES = new ConcurrentHashMap<>();

    public static void cacheUpdate(CosmeticUpdateMessage message) {
        PENDING_UPDATES.computeIfAbsent(message.getPlayerUuid(), k -> new ConcurrentHashMap<>())
            .put(message.getSlot(), message);
    }

    public static Map<String, CosmeticUpdateMessage> getCachedUpdates(UUID playerUuid) {
        return PENDING_UPDATES.getOrDefault(playerUuid, new ConcurrentHashMap<>());
    }

    public static Map<String, CosmeticUpdateMessage> consumeCachedUpdates(UUID playerUuid) {
        return PENDING_UPDATES.remove(playerUuid);
    }

    public static void removeCachedUpdates(UUID playerUuid) {
        PENDING_UPDATES.remove(playerUuid);
    }

    public static boolean hasCachedUpdates(UUID playerUuid) {
        return PENDING_UPDATES.containsKey(playerUuid) && !PENDING_UPDATES.get(playerUuid).isEmpty();
    }

    public static void clear() {
        PENDING_UPDATES.clear();
    }
}
