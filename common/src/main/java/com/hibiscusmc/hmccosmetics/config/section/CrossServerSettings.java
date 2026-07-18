package com.hibiscusmc.hmccosmetics.config.section;

import lombok.Getter;
import me.lojosho.shaded.configurate.ConfigurationNode;

public class CrossServerSettings {

    private static final String CROSS_SERVER_PATH = "cross-server";
    private static final String ENABLED_PATH = "enabled";
    private static final String TRANSPORT_PATH = "transport";
    private static final String REDIS_PATH = "redis";
    private static final String REDIS_HOST_PATH = "host";
    private static final String REDIS_PORT_PATH = "port";
    private static final String REDIS_PASSWORD_PATH = "password";
    private static final String REDIS_DATABASE_PATH = "database";
    private static final String PLUGIN_MESSAGING_PATH = "plugin-messaging";
    private static final String PLUGIN_MESSAGING_CHANNEL_PATH = "channel";
    private static final String SERVER_ID_PATH = "server-id";

    @Getter
    private static boolean enabled;
    @Getter
    private static String transport;
    @Getter
    private static String redisHost;
    @Getter
    private static int redisPort;
    @Getter
    private static String redisPassword;
    @Getter
    private static int redisDatabase;
    @Getter
    private static String pluginMessagingChannel;
    @Getter
    private static String serverId;

    public static void load(ConfigurationNode source) {
        ConfigurationNode crossServer = source.node(CROSS_SERVER_PATH);
        enabled = crossServer.node(ENABLED_PATH).getBoolean(false);
        transport = crossServer.node(TRANSPORT_PATH).getString("redis");
        serverId = crossServer.node(SERVER_ID_PATH).getString("server-1");

        ConfigurationNode redis = crossServer.node(REDIS_PATH);
        redisHost = redis.node(REDIS_HOST_PATH).getString("localhost");
        redisPort = redis.node(REDIS_PORT_PATH).getInt(6379);
        redisPassword = redis.node(REDIS_PASSWORD_PATH).getString("");
        redisDatabase = redis.node(REDIS_DATABASE_PATH).getInt(0);

        ConfigurationNode pluginMessaging = crossServer.node(PLUGIN_MESSAGING_PATH);
        pluginMessagingChannel = pluginMessaging.node(PLUGIN_MESSAGING_CHANNEL_PATH).getString("hmccosmetics:sync");
    }

    public static boolean isRedisTransport() {
        return "redis".equalsIgnoreCase(transport);
    }

    public static boolean isPluginMessagingTransport() {
        return "plugin-messaging".equalsIgnoreCase(transport);
    }
}
