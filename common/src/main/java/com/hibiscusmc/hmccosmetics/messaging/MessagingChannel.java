package com.hibiscusmc.hmccosmetics.messaging;

import com.hibiscusmc.hmccosmetics.messaging.message.CrossServerMessage;

public interface MessagingChannel {
    void connect();
    void disconnect();
    void publish(CrossServerMessage message);
    void subscribe(MessageHandler handler);
    boolean isConnected();

    @FunctionalInterface
    interface MessageHandler {
        void onMessage(CrossServerMessage message);
    }
}
