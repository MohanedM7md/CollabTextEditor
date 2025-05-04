package com.editor.collabtexteditor.controllers;

import com.editor.collabtexteditor.model.*;
import com.editor.collabtexteditor.Networking.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollabSessionController {
    private final BorderPane root = new BorderPane();
    private final TextArea textArea = new TextArea();
    private final Label statusLabel = new Label("Status: Not connected");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HBox sessionBar = new HBox(8);
    private final TextField userIdField = new TextField();
    private final TextField docIdField = new TextField();
    private final ToggleGroup roleGroup = new ToggleGroup();
    private final Button connectBtn = new Button("Join Document");
    private final Button createDocBtn = new Button("Create New");
    private final VBox rightBar = new VBox(12);
    private final Button undoBtn = new Button("Undo");
    private final Button redoBtn = new Button("Redo");

    private CollaborationWebSocket webSocketClient;
    private String userId;
    private String docId;
    private String userMode;
    private final Map<String, Integer> remoteCursors = new ConcurrentHashMap<>();
    private final Map<String, String> cursorColors = new ConcurrentHashMap<>();

    public CollabSessionController() {
        initializeUI();
        setupEventHandlers();
    }

    private void initializeUI() {
        // Session Bar
        userIdField.setPromptText("User ID");
        docIdField.setPromptText("Share Code");

        RadioButton editorBtn = new RadioButton("Editor");
        editorBtn.setToggleGroup(roleGroup);
        editorBtn.setSelected(true);
        RadioButton viewerBtn = new RadioButton("Viewer");
        viewerBtn.setToggleGroup(roleGroup);

        sessionBar.getChildren().addAll(
                new Label("User:"), userIdField,
                new Label("Share Code:"), docIdField,
                editorBtn, viewerBtn,
                connectBtn, createDocBtn
        );
        sessionBar.setPadding(new Insets(10));
        sessionBar.getStyleClass().add("session-bar");

        // Text Area
        textArea.setWrapText(true);
        textArea.setDisable(true);
        textArea.getStyleClass().add("text-area");

        VBox centerBox = new VBox(8, textArea, statusLabel);
        centerBox.setPadding(new Insets(10));
        statusLabel.getStyleClass().add("status-label");

        // Right Bar
        undoBtn.getStyleClass().add("button");
        redoBtn.getStyleClass().add("button");
        rightBar.getChildren().addAll(undoBtn, redoBtn);
        rightBar.setPadding(new Insets(10));

        // Root Layout
        root.setTop(sessionBar);
        root.setCenter(centerBox);
        root.setRight(rightBar);
    }

    private void setupEventHandlers() {
        connectBtn.setOnAction(event -> {
            userId = userIdField.getText().trim();
            docId = docIdField.getText().trim();
            userMode = roleGroup.getSelectedToggle() == null ? "editor" :
                    ((RadioButton)roleGroup.getSelectedToggle()).getText().toLowerCase();

            if(userId.isEmpty() || docId.isEmpty()) {
                statusLabel.setText("Please enter a user ID and share code.");
                return;
            }
            connectToSession();
        });

        createDocBtn.setOnAction(event -> createNewDocument());

        textArea.textProperty().addListener((obs, oldText, newText) -> {
            if (webSocketClient == null || !textArea.isEditable()) return;

            int changePos = findChangePosition(oldText, newText);
            if (changePos >= 0) {
                if (newText.length() > oldText.length() && newText.length() > 0) {
                    char c = newText.charAt(changePos);
                    InsertRequest req = new InsertRequest(userId, c, changePos);
                    sendMessage("/app/document/" + docId + "/insert", req);
                } else if (oldText.length() > newText.length()) {
                    DeleteRequest req = new DeleteRequest(userId, changePos);
                    sendMessage("/app/document/" + docId + "/delete", req);
                }
            }
        });

        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (webSocketClient == null || !textArea.isEditable()) return;
            CursorUpdateRequest req = new CursorUpdateRequest(userId, newPos.intValue(), getColorForUser(userId));
            sendMessage("/app/document/" + docId + "/cursor", req);
        });

        undoBtn.setOnAction(e -> {
            if(webSocketClient == null) return;
            sendMessage("/app/document/" + docId + "/undo", new UndoRequest(userId));
        });

        redoBtn.setOnAction(e -> {
            if(webSocketClient == null) return;
            sendMessage("/app/document/" + docId + "/redo", new RedoRequest(userId));
        });
    }

    private void createNewDocument() {
        userId = userIdField.getText().trim();
        if (userId.isEmpty()) {
            statusLabel.setText("Please enter a User ID first");
            return;
        }

        try {
            String createUrl = "http://localhost:8080/api/documents/create?userId=" + userId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(createUrl))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                Document doc = objectMapper.readValue(response.body(), Document.class);
                                Platform.runLater(() -> {
                                    docIdField.setText(doc.getId());
                                    statusLabel.setText("Created new document: " + doc.getId());
                                });
                            } catch (Exception e) {
                                Platform.runLater(() ->
                                        statusLabel.setText("Error parsing response: " + e.getMessage()));
                            }
                        } else {
                            Platform.runLater(() ->
                                    statusLabel.setText("Failed to create document: " + response.body()));
                        }
                    });
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void connectToSession() {
        try {
            String wsUrl = "ws://localhost:8080/ws/editor";
            webSocketClient = new CollaborationWebSocket(
                    new URI(wsUrl),
                    this::handleServerMessage,
                    () -> updateConnectionStatus(false)
            );
            webSocketClient.connectBlocking();

            ConnectRequest joinReq = new ConnectRequest(userId, docId);
            sendMessage("/app/document/connect", joinReq);

            textArea.setDisable(false);
            textArea.setEditable("editor".equals(userMode));
            updateConnectionStatus(true);

        } catch (Exception e) {
            statusLabel.setText("Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMessage(String path, Object body) {
        if(webSocketClient != null && webSocketClient.isOpen()) {
            try {
                String payload = objectMapper.writeValueAsString(body);
                webSocketClient.send(path + " " + payload);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleServerMessage(String msg) {
        try {
            if (msg.contains("\"operationType\"")) {
                DocumentStateResponse resp = objectMapper.readValue(msg, DocumentStateResponse.class);
                Platform.runLater(() -> {
                    if (!textArea.getText().equals(resp.getText())) {
                        textArea.setText(resp.getText());
                    }
                    visualizeRemoteCursors();
                });
            } else if (msg.contains("\"userId\"") && msg.contains("\"color\"") && msg.contains("\"position\"")) {
                CursorResponse resp = objectMapper.readValue(msg, CursorResponse.class);
                Platform.runLater(() -> {
                    remoteCursors.put(resp.getUserId(), resp.getPosition());
                    cursorColors.put(resp.getUserId(), resp.getColor());
                    visualizeRemoteCursors();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void visualizeRemoteCursors() {
        StringBuilder styleBuilder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : remoteCursors.entrySet()) {
            String userId = entry.getKey();
            int pos = entry.getValue();
            String color = cursorColors.getOrDefault(userId, "#FF0000");

            styleBuilder.append(String.format(
                    "-rtfx-background-color: %s;",
                    color + "55"
            ));
        }

        textArea.setStyle(styleBuilder.toString());
    }

    private void updateConnectionStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                statusLabel.setText("Connected to document: " + docId);
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                statusLabel.setText("Disconnected");
                statusLabel.setStyle("-fx-text-fill: red;");
                textArea.setDisable(true);
            }
        });
    }

    private int findChangePosition(String oldText, String newText) {
        int min = Math.min(oldText.length(), newText.length());
        for (int i = 0; i < min; i++) {
            if (oldText.charAt(i) != newText.charAt(i)) return i;
        }
        if (oldText.length() < newText.length()) return newText.length() - 1;
        else if (oldText.length() > newText.length()) return min;
        else return -1;
    }

    private String getColorForUser(String userId) {
        int code = userId.hashCode();
        Color color = Color.hsb((code % 360 + 360) % 360, 0.60, 0.8);
        return String.format("#%02X%02X%02X",
                (int)(color.getRed()*255),
                (int)(color.getGreen()*255),
                (int)(color.getBlue()*255));
    }

    public BorderPane getRoot() {
        return root;
    }
}