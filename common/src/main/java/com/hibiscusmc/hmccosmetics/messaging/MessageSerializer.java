package com.hibiscusmc.hmccosmetics.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hibiscusmc.hmccosmetics.messaging.message.CrossServerMessage;
import com.hibiscusmc.hmccosmetics.messaging.message.CosmeticUpdateMessage;
import com.hibiscusmc.hmccosmetics.messaging.message.PlayerDataSyncMessage;
import com.hibiscusmc.hmccosmetics.messaging.message.PlayerSessionMessage;

import java.util.UUID;

public class MessageSerializer {

    private static final Gson GSON = new GsonBuilder().create();

    public static String serialize(CrossServerMessage message) {
        JsonObject json = new JsonObject();
        json.addProperty("type", message.getType());
        json.addProperty("serverId", message.getServerId());

        if (message instanceof CosmeticUpdateMessage cosmeticMsg) {
            json.addProperty("playerUuid", cosmeticMsg.getPlayerUuid().toString());
            json.addProperty("slot", cosmeticMsg.getSlot());
            json.addProperty("cosmeticId", cosmeticMsg.getCosmeticId());
            json.addProperty("color", cosmeticMsg.getColor());
            json.addProperty("remove", cosmeticMsg.isRemove());
        } else if (message instanceof PlayerSessionMessage sessionMsg) {
            json.addProperty("playerUuid", sessionMsg.getPlayerUuid().toString());
            json.addProperty("action", sessionMsg.getAction().name());
        } else if (message instanceof PlayerDataSyncMessage syncMsg) {
            json.addProperty("playerUuid", syncMsg.getPlayerUuid().toString());
            json.addProperty("serializedData", syncMsg.getSerializedData());
            json.addProperty("isResponse", syncMsg.isResponse());
        }

        return GSON.toJson(json);
    }

    public static CrossServerMessage deserialize(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        String serverId = jsonObject.get("serverId").getAsString();

        return switch (type) {
            case "COSMETIC_UPDATE" -> new CosmeticUpdateMessage(
                serverId,
                UUID.fromString(jsonObject.get("playerUuid").getAsString()),
                jsonObject.get("slot").getAsString(),
                jsonObject.get("cosmeticId").getAsString(),
                jsonObject.get("color").getAsInt(),
                jsonObject.get("remove").getAsBoolean()
            );
            case "PLAYER_JOIN", "PLAYER_QUIT" -> new PlayerSessionMessage(
                serverId,
                UUID.fromString(jsonObject.get("playerUuid").getAsString()),
                PlayerSessionMessage.SessionAction.valueOf(jsonObject.get("action").getAsString())
            );
            case "DATA_SYNC_REQUEST", "DATA_SYNC_RESPONSE" -> new PlayerDataSyncMessage(
                serverId,
                UUID.fromString(jsonObject.get("playerUuid").getAsString()),
                jsonObject.get("serializedData").getAsString(),
                jsonObject.get("isResponse").getAsBoolean()
            );
            default -> null;
        };
    }
}
