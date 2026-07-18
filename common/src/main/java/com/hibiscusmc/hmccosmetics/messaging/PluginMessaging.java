package com.hibiscusmc.hmccosmetics.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.section.CrossServerSettings;
import com.hibiscusmc.hmccosmetics.messaging.message.CrossServerMessage;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class PluginMessaging implements MessagingChannel, PluginMessageListener, Listener {

    private MessageHandler handler;
    private boolean connected = false;
    private final String channel;

    public PluginMessaging() {
        this.channel = CrossServerSettings.getPluginMessagingChannel();
    }

    @Override
    public void connect() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(HMCCosmeticsPlugin.getInstance(), channel);
        Bukkit.getMessenger().registerIncomingPluginChannel(HMCCosmeticsPlugin.getInstance(), channel, this);
        Bukkit.getPluginManager().registerEvents(this, HMCCosmeticsPlugin.getInstance());
        connected = true;
        MessagesUtil.sendDebugMessages("Plugin messaging registered on channel: " + channel);
    }

    @Override
    public void disconnect() {
        connected = false;
        try {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(HMCCosmeticsPlugin.getInstance(), channel);
            Bukkit.getMessenger().unregisterIncomingPluginChannel(HMCCosmeticsPlugin.getInstance(), channel, this);
        } catch (Exception e) {
            MessagesUtil.sendDebugMessages("Error unregistering plugin messaging: " + e.getMessage());
        }
    }

    @Override
    public void publish(CrossServerMessage message) {
        if (!connected) return;

        Player player = getFirstOnlinePlayer();
        if (player == null) return;

        try {
            byte[] data = MessageSerializer.serialize(message).getBytes();
            player.sendPluginMessage(HMCCosmeticsPlugin.getInstance(), channel, data);
        } catch (Exception e) {
            MessagesUtil.sendDebugMessages("Failed to send plugin message: " + e.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(this.channel)) return;
        if (handler == null) return;

        try {
            String json = new String(message);
            CrossServerMessage crossServerMessage = MessageSerializer.deserialize(json);
            if (crossServerMessage != null) {
                handler.onMessage(crossServerMessage);
            }
        } catch (Exception e) {
            MessagesUtil.sendDebugMessages("Error processing plugin message: " + e.getMessage());
        }
    }

    @Override
    public void subscribe(MessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!connected) return;
        // Re-register channels when a player joins (required for plugin messaging)
        try {
            Bukkit.getMessenger().registerOutgoingPluginChannel(HMCCosmeticsPlugin.getInstance(), channel);
            Bukkit.getMessenger().registerIncomingPluginChannel(HMCCosmeticsPlugin.getInstance(), channel, this);
        } catch (Exception e) {
            // Already registered
        }
    }

    private Player getFirstOnlinePlayer() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            return player;
        }
        return null;
    }
}
