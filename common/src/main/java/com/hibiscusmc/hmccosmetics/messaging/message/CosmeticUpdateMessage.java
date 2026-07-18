package com.hibiscusmc.hmccosmetics.messaging.message;

import lombok.Getter;

import java.util.UUID;

@Getter
public class CosmeticUpdateMessage extends CrossServerMessage {

    private final UUID playerUuid;
    private final String slot;
    private final String cosmeticId;
    private final int color;
    private final boolean remove;

    public CosmeticUpdateMessage(String serverId, UUID playerUuid, String slot, String cosmeticId, int color, boolean remove) {
        super(MessageType.COSMETIC_UPDATE.name(), serverId);
        this.playerUuid = playerUuid;
        this.slot = slot;
        this.cosmeticId = cosmeticId;
        this.color = color;
        this.remove = remove;
    }
}
