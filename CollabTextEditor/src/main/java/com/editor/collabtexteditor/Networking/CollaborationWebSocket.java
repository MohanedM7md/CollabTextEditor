package com.editor.collabtexteditor.Networking;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class CollaborationWebSocket extends WebSocketClient {
    private final Consumer<String> messageHandler;
    private final Runnable onDisconnect;
    private static final boolean DEBUG = true;
    public CollaborationWebSocket(URI uri, Consumer<String> messageHandler, Runnable onDisconnect) {
        super(uri);
        this.messageHandler = messageHandler;
        this.onDisconnect = onDisconnect;
        if (DEBUG) System.out.println("[WebSocket] Initialized for URI: "+ uri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        if (DEBUG) {
            System.out.println("[WebSocket] Connection opened");
            System.out.println("Status: " + handshake.getHttpStatus());
            System.out.println("Status Message: " + handshake.getHttpStatusMessage());
            System.out.println("Headers:");
            handshake.iterateHttpFields().forEachRemaining(
                    header -> System.out.println("  " + header + ": " + handshake.getFieldValue(header))
            );
        }
    }

    @Override
    public void onMessage(String message) {
        if (DEBUG) System.out.println("[WebSocket] Received: " + message);
        messageHandler.accept(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (DEBUG) {
            System.out.println("[WebSocket] Connection closed");
            System.out.println("Code: " + code);
            System.out.println("Reason: " + reason);
            System.out.println("Remote: " + remote);
        }
        onDisconnect.run();
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}