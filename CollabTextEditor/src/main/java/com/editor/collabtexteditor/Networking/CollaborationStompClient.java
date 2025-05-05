package com.editor.collabtexteditor.Networking;

import com.editor.collabtexteditor.model.CursorResponse;
import com.editor.collabtexteditor.model.DocumentStateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.awt.*;
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
    private final TextArea textArea=new TextArea();


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
                session.subscribe("/topic/document/" + docId + "/user-left", new DocumentUpdateHandler());
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
                CursorResponse response = (CursorResponse) payload;
                CursorResponse safeResponse = makePositionSafe(response);

                String json = objectMapper.writeValueAsString(safeResponse);
                messageHandler.accept(json);

            } catch (JsonProcessingException e) {
                System.err.println("JSON processing error in cursor update:");
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("Unexpected error handling cursor update:");
                e.printStackTrace();
            }
        }

        private CursorResponse makePositionSafe(CursorResponse response) {
            String currentText = textArea.getText();
            int textLength = currentText.length();
            int position = response.getPosition();

            // Adjust position if out of bounds
            if (position < 0) {
                position = 0;
            } else if (position > textLength) {
                position = textLength;
            }

            // Ensure position isn't in the middle of a line break
            if (position > 0 && position < textLength) {
                char prevChar = currentText.charAt(position - 1);
                char nextChar = currentText.charAt(position);
                if (prevChar == '\r' && nextChar == '\n') {
                    position--; // Move to start of line break
                }
            }

            // Return new response with safe position
            return new CursorResponse(
                    response.getUserId(),response.getPosition(),
                    response.getColor()
            );
        }
    }


    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }
}