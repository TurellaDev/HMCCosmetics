package com.hibiscusmc.hmccosmetics.messaging.message;

import lombok.Getter;

import java.util.UUID;

@Getter
public class PlayerSessionMessage extends CrossServerMessage {

    private final UUID playerUuid;
    private final SessionAction action;

    public PlayerSessionMessage(String serverId, UUID playerUuid, SessionAction action) {
        super(MessageType.PLAYER_JOIN.name(), serverId);
        this.playerUuid = playerUuid;
        this.action = action;
    }

    public enum SessionAction {
        JOIN,
        QUIT
    }
}
