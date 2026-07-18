package com.hibiscusmc.hmccosmetics.messaging.message;

import lombok.Getter;

import java.util.UUID;

public abstract class CrossServerMessage {

    @Getter
    private final String type;

    @Getter
    private final String serverId;

    protected CrossServerMessage(String type, String serverId) {
        this.type = type;
        this.serverId = serverId;
    }

    public enum MessageType {
        COSMETIC_UPDATE,
        PLAYER_JOIN,
        PLAYER_QUIT,
        DATA_SYNC_REQUEST,
        DATA_SYNC_RESPONSE
    }
}
