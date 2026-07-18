package com.hibiscusmc.hmccosmetics.messaging;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.cache.CosmeticCache;
import com.hibiscusmc.hmccosmetics.config.section.CrossServerSettings;
import com.hibiscusmc.hmccosmetics.messaging.message.CrossServerMessage;
import com.hibiscusmc.hmccosmetics.messaging.message.CosmeticUpdateMessage;
import com.hibiscusmc.hmccosmetics.messaging.message.PlayerDataSyncMessage;
import com.hibiscusmc.hmccosmetics.messaging.message.PlayerSessionMessage;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetics;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.database.Database;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CrossServerManager {

    @Getter
    private static CrossServerManager instance;
    @Getter
    private static MessagingChannel messagingChannel;
    private static boolean enabled = false;

    public CrossServerManager() {
        instance = this;
    }

    public void initialize() {
        if (!CrossServerSettings.isEnabled()) {
            MessagesUtil.sendDebugMessages("Cross-server messaging is disabled");
            return;
        }

        if (CrossServerSettings.isRedisTransport()) {
            messagingChannel = new RedisMessaging();
        } else if (CrossServerSettings.isPluginMessagingTransport()) {
            messagingChannel = new PluginMessaging();
        } else {
            MessagesUtil.sendDebugMessages("Invalid transport type: " + CrossServerSettings.getTransport(), Level.WARNING);
            return;
        }

        messagingChannel.subscribe(this::handleMessage);
        messagingChannel.connect();
        enabled = true;
        MessagesUtil.sendDebugMessages("Cross-server messaging initialized with " + CrossServerSettings.getTransport());
    }

    public void shutdown() {
        if (messagingChannel != null) {
            messagingChannel.disconnect();
        }
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled && messagingChannel != null && messagingChannel.isConnected();
    }

    private void handleMessage(CrossServerMessage message) {
        if (message.getServerId().equals(CrossServerSettings.getServerId())) {
            return; // Ignore messages from same server
        }

        switch (message.getType()) {
            case "COSMETIC_UPDATE" -> handleCosmeticUpdate((CosmeticUpdateMessage) message);
            case "PLAYER_JOIN" -> handlePlayerJoin((PlayerSessionMessage) message);
            case "PLAYER_QUIT" -> handlePlayerQuit((PlayerSessionMessage) message);
            case "DATA_SYNC_REQUEST" -> handleDataSyncRequest((PlayerDataSyncMessage) message);
            case "DATA_SYNC_RESPONSE" -> handleDataSyncResponse((PlayerDataSyncMessage) message);
        }
    }

    private void handleCosmeticUpdate(CosmeticUpdateMessage message) {
        UUID playerUuid = message.getPlayerUuid();
        CosmeticUser user = CosmeticUsers.getUser(playerUuid);

        if (user != null) {
            // Player is online on this server, apply update directly
            applyCosmeticUpdate(user, message);
        } else {
            // Player not on this server, cache for when they join
            CosmeticCache.cacheUpdate(message);
        }
    }

    public void applyCosmeticUpdate(CosmeticUser user, CosmeticUpdateMessage message) {
        try {
            CosmeticSlot slot = CosmeticSlot.valueOf(message.getSlot());

            if (message.isRemove()) {
                user.removeCosmeticSlot(slot);
            } else {
                Cosmetic cosmetic = Cosmetics.getCosmetic(message.getCosmeticId());
                if (cosmetic != null) {
                    Color color = message.getColor() != -1 ? Color.fromRGB(message.getColor()) : null;
                    user.addCosmetic(cosmetic, color);
                }
            }
        } catch (Exception e) {
            MessagesUtil.sendDebugMessages("Error applying cosmetic update: " + e.getMessage(), Level.WARNING);
        }
    }

    private void handlePlayerJoin(PlayerSessionMessage message) {
        // Player joined another server, remove any cached data
        CosmeticCache.removeCachedUpdates(message.getPlayerUuid());
    }

    private void handlePlayerQuit(PlayerSessionMessage message) {
        // Player quit another server, nothing to do here
    }

    private void handleDataSyncRequest(PlayerDataSyncMessage message) {
        if (message.isResponse()) return;

        UUID playerUuid = message.getPlayerUuid();
        CosmeticUser user = CosmeticUsers.getUser(playerUuid);

        if (user != null) {
            String serializedData = Database.getData().serializeData(user);
            PlayerDataSyncMessage response = new PlayerDataSyncMessage(
                CrossServerSettings.getServerId(),
                playerUuid,
                serializedData,
                true
            );
            messagingChannel.publish(response);
        }
    }

    private void handleDataSyncResponse(PlayerDataSyncMessage message) {
        UUID playerUuid = message.getPlayerUuid();
        CosmeticUser user = CosmeticUsers.getUser(playerUuid);

        if (user != null) {
            // Apply the synced data
            var cosmetics = Database.getData().deserializeData(message.getSerializedData());
            // TODO: Apply to user
        }
    }

    public void publishCosmeticUpdate(CosmeticUser user, CosmeticSlot slot, boolean remove) {
        if (!isEnabled()) return;

        Cosmetic cosmetic = user.getCosmetic(slot);
        int color = -1;
        if (cosmetic != null) {
            Color cosmeticColor = user.getCosmeticColor(slot);
            if (cosmeticColor != null) {
                color = cosmeticColor.asRGB();
            }
        }

        CosmeticUpdateMessage message = new CosmeticUpdateMessage(
            CrossServerSettings.getServerId(),
            user.getUniqueId(),
            slot.getName(),
            cosmetic != null ? cosmetic.getId() : null,
            color,
            remove
        );

        messagingChannel.publish(message);
    }

    public void publishPlayerJoin(UUID playerUuid) {
        if (!isEnabled()) return;

        PlayerSessionMessage message = new PlayerSessionMessage(
            CrossServerSettings.getServerId(),
            playerUuid,
            PlayerSessionMessage.SessionAction.JOIN
        );

        messagingChannel.publish(message);
    }

    public void publishPlayerQuit(UUID playerUuid) {
        if (!isEnabled()) return;

        PlayerSessionMessage message = new PlayerSessionMessage(
            CrossServerSettings.getServerId(),
            playerUuid,
            PlayerSessionMessage.SessionAction.QUIT
        );

        messagingChannel.publish(message);
    }

    public void requestPlayerDataSync(UUID playerUuid) {
        if (!isEnabled()) return;

        PlayerDataSyncMessage message = new PlayerDataSyncMessage(
            CrossServerSettings.getServerId(),
            playerUuid,
            "",
            false
        );

        messagingChannel.publish(message);
    }
}
