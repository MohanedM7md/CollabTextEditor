package com.editor.collabtexteditor.Networking;

import com.editor.collabtexteditor.model.CursorResponse;
import com.editor.collabtexteditor.model.DocumentStateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class CollaborationStompClient {
    private StompSession stompSession;
    private final String serverUrl;
    private final Consumer<String> messageHandler;
    private final Runnable connectionClosedHandler;
    private final String docId;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public CollaborationStompClient(String serverUrl,
                                    String docId,
                                    Consumer<String> messageHandler,
                                    Runnable connectionClosedHandler) {
        this.serverUrl = serverUrl;
        this.docId = docId;
        this.messageHandler = messageHandler;
        this.connectionClosedHandler = connectionClosedHandler;
    }

    public void connect() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(webSocketClient));
        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        this.stompSession = stompClient.connect(serverUrl, new StompSessionHandler() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("[WebSocket] Connected to server");

                // Subscribe to real-time updates
                session.subscribe("/topic/document/" + docId + "/state", new DocumentUpdateHandler());
                session.subscribe("/topic/document/" + docId + "/cursors", new CursorUpdateHandler());
            }

            @Override
            public void handleException(StompSession session, StompCommand command,
                                        StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("STOMP exception: " + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                System.err.println("[WebSocket] Transport error: " + exception.getMessage());
                connectionClosedHandler.run(); // Optionally reconnect or notify user
            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Not used for session handler
            }
        }).get(10, TimeUnit.SECONDS);
    }

    public void send(String destination, Object payload) {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send(destination, payload);
        } else {
            System.err.println("Cannot send message - not connected");
        }
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    private class DocumentUpdateHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return DocumentStateResponse.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                messageHandler.accept(json);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }


    private class CursorUpdateHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return CursorResponse.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                messageHandler.accept(json);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }
}