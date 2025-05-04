package com.editor.collabtexteditor.Networking;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class CollaborationWebSocket extends WebSocketClient {
    private final Consumer<String> messageHandler;
    private final Runnable onDisconnect;

    public CollaborationWebSocket(URI uri, Consumer<String> messageHandler, Runnable onDisconnect) {
        super(uri);
        this.messageHandler = messageHandler;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to server");
    }

    @Override
    public void onMessage(String message) {
        messageHandler.accept(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket closed: " + reason);
        onDisconnect.run();
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}