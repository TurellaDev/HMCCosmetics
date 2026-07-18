package com.hibiscusmc.hmccosmetics.messaging.message;

import lombok.Getter;

import java.util.UUID;

@Getter
public class PlayerDataSyncMessage extends CrossServerMessage {

    private final UUID playerUuid;
    private final String serializedData;
    private final boolean isResponse;

    public PlayerDataSyncMessage(String serverId, UUID playerUuid, String serializedData, boolean isResponse) {
        super(MessageType.DATA_SYNC_REQUEST.name(), serverId);
        this.playerUuid = playerUuid;
        this.serializedData = serializedData;
        this.isResponse = isResponse;
    }
}
