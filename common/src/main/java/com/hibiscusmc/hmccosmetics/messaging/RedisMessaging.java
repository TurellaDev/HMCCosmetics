package com.hibiscusmc.hmccosmetics.messaging;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.section.CrossServerSettings;
import com.hibiscusmc.hmccosmetics.messaging.message.CrossServerMessage;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class RedisMessaging implements MessagingChannel {

    private static final String CHANNEL = "hmccosmetics:sync";
    private JedisPool jedisPool;
    private JedisPubSub pubSub;
    private Thread subscriberThread;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MessageHandler handler;
    private volatile boolean connected = false;

    @Override
    public void connect() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(16);
            poolConfig.setMaxIdle(8);
            poolConfig.setMinIdle(2);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);

            String password = CrossServerSettings.getRedisPassword();
            if (password == null || password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, CrossServerSettings.getRedisHost(), CrossServerSettings.getRedisPort(), 5000);
            } else {
                jedisPool = new JedisPool(poolConfig, CrossServerSettings.getRedisHost(), CrossServerSettings.getRedisPort(), 5000, password);
            }

            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }

            connected = true;
            MessagesUtil.sendDebugMessages("Redis connection established to " + CrossServerSettings.getRedisHost() + ":" + CrossServerSettings.getRedisPort());

            startSubscriber();
        } catch (Exception e) {
            MessagesUtil.sendDebugMessages("Failed to connect to Redis: " + e.getMessage(), Level.SEVERE);
            connected = false;
        }
    }

    private void startSubscriber() {
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (handler == null) return;
                try {
                    CrossServerMessage crossServerMessage = MessageSerializer.deserialize(message);
                    if (crossServerMessage != null && handler != null) {
                        handler.onMessage(crossServerMessage);
                    }
                } catch (Exception e) {
                    MessagesUtil.sendDebugMessages("Error processing Redis message: " + e.getMessage(), Level.WARNING);
                }
            }
        };

        subscriberThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(pubSub, CHANNEL);
            } catch (Exception e) {
                if (connected) {
                    MessagesUtil.sendDebugMessages("Redis subscriber disconnected: " + e.getMessage(), Level.WARNING);
                    connected = false;
                }
            }
        }, "HMCCosmetics-Redis-Subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    @Override
    public void disconnect() {
        connected = false;
        if (pubSub != null && !pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }
        if (subscriberThread != null) {
            subscriberThread.interrupt();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
        executor.shutdown();
        MessagesUtil.sendDebugMessages("Redis connection closed");
    }

    @Override
    public void publish(CrossServerMessage message) {
        if (!connected || jedisPool == null || jedisPool.isClosed()) return;

        executor.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String serialized = MessageSerializer.serialize(message);
                jedis.publish(CHANNEL, serialized);
            } catch (Exception e) {
                MessagesUtil.sendDebugMessages("Failed to publish Redis message: " + e.getMessage(), Level.WARNING);
            }
        });
    }

    @Override
    public void subscribe(MessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean isConnected() {
        return connected && jedisPool != null && !jedisPool.isClosed();
    }
}
