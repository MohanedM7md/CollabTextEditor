package com.editor.collabtexteditor.controllers;

import com.editor.collabtexteditor.Networking.CollaborationStompClient;
import com.editor.collabtexteditor.model.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.editor.collabtexteditor.Configs.API_URL;

public class CollabSessionController {
    private final BorderPane root = new BorderPane();
    private final TextArea textArea = new TextArea();
    private final Label statusLabel = new Label("Status: Loading...");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HBox sessionBar = new HBox(10);
    private final Button backBtn = new Button("← Back to Documents");
    private final VBox rightBar = new VBox(12);
    private final Button undoBtn = new Button("Undo");
    private final Button redoBtn = new Button("Redo");
    private final Button shareBtn = new Button("Share");
    private final Label docInfoLabel = new Label();
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private final ListView<String> activeUsersList = new ListView<>();
    private final VBox activeUsersBox = new VBox(5
            ,new Label(),
            activeUsersList);
    private CollaborationStompClient stompClient;  // Add this line
    private final String docId;
    private final String userId;
    private final boolean isEditor;
    private final Map<String, Integer> remoteCursors = new ConcurrentHashMap<>();
    private final Map<String, String> cursorColors = new ConcurrentHashMap<>();
    private final Map<String, String> activeUsers = new ConcurrentHashMap<>();
    private boolean isApplyingRemoteUpdate = false;

    // For REST API calls
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Document metadata
    private String documentTitle = "";
    private String documentCreator = "";
    private String lastModified = "";
    private String viewerCode = "";
    private String editorCode = "";
    private Consumer<String> messageHandler;

    public CollabSessionController(String docId, String title, String userId, boolean isEditor) {
        this.docId = docId;
        this.userId = userId;
        this.isEditor = isEditor;
        Label titleLabel = new Label();
        titleLabel.setText(title);

        applyCssStyles();
        initializeUI();
        setupEventHandlers();
        loadDocument();
    }

    private void applyCssStyles() {
        // Root styling
        root.setStyle("-fx-background-color: #F5F7FA;");

        // Text Area styling
        textArea.setStyle(
                "-fx-font-family: 'Consolas', 'Monaco', monospace;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-color: white;" +
                        "-fx-border-color: #DEE2E6;" +
                        "-fx-border-radius: 5px;" +
                        "-fx-padding: 10px;" +
                        "-fx-line-spacing: 1.5;"
        );

        // Session bar styling
        sessionBar.setStyle(
                "-fx-background-color: #343A40;" +
                        "-fx-padding: 15px;" +
                        "-fx-border-color: #2A2E33;" +
                        "-fx-border-width: 0 0 1 0;"
        );

        // Back button styling
        backBtn.setStyle(
                "-fx-background-color: #6C757D;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-cursor: hand;"
        );

        // Doc info label styling
        docInfoLabel.setStyle(
                "-fx-text-fill: #F8F9FA;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );

        // Status label styling
        statusLabel.setStyle(
                "-fx-text-fill: #ADB5BD;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 0 0 5 0;"
        );

        // Right sidebar styling
        rightBar.setStyle(
                "-fx-background-color: #E9ECEF;" +
                        "-fx-padding: 15px;" +
                        "-fx-border-color: #DEE2E6;" +
                        "-fx-border-width: 0 0 0 1px;"
        );

        // Button styling
        String buttonStyle =
                "-fx-background-color: #007BFF;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10px 15px;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-cursor: hand;";

        shareBtn.setStyle(buttonStyle.replace("#007BFF", "#28A745"));
        undoBtn.setStyle(buttonStyle);
        redoBtn.setStyle(buttonStyle);

        // Loading indicator styling
        loadingIndicator.setStyle("-fx-progress-color: #007BFF;");
        loadingIndicator.setPrefSize(20, 20);

        // Active users box styling
        activeUsersBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 10px;" +
                        "-fx-border-color: #DEE2E6;" +
                        "-fx-border-radius: 5px;"
        );
    }

    private void initializeUI() {
        // Session Info Bar
        HBox.setHgrow(docInfoLabel, Priority.ALWAYS);
        docInfoLabel.setText("Document: " + docId);

        // Add loading indicator to status bar
        HBox statusBox = new HBox(10, loadingIndicator, statusLabel);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        sessionBar.getChildren().addAll(backBtn, docInfoLabel, statusBox);
        sessionBar.setAlignment(Pos.CENTER_LEFT);

        // Text Area
        textArea.setWrapText(true);
        textArea.setEditable(false); // Initially disabled until document loads
        textArea.setPrefHeight(500);

        // Add line numbers (simplified version)
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Right Toolbar
        Label toolsHeader = new Label("DOCUMENT TOOLS");
        toolsHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6C757D;");

        // Enhance buttons with icons (text representation)
        shareBtn.setText("📤 Share");
        undoBtn.setText("↩ Undo");
        redoBtn.setText("↪ Redo");

        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));

        // Document info section
        VBox docMetadataBox = new VBox(5);
        docMetadataBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 10px;" +
                        "-fx-border-color: #DEE2E6;" +
                        "-fx-border-radius: 5px;"
        );

        Label metadataHeader = new Label("DOCUMENT INFO");
        metadataHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6C757D;");

        Label titleLabel = new Label("Loading...");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label creatorLabel = new Label("Creator: Loading...");
        Label modifiedLabel = new Label("Last modified: Loading...");

        docMetadataBox.getChildren().addAll(titleLabel, creatorLabel, modifiedLabel);

        // Active users section
        Label usersHeader = new Label("ACTIVE USERS");
        usersHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6C757D;");

        // Main Layout
        VBox centerBox = new VBox(10, scrollPane, statusLabel);
        centerBox.setPadding(new Insets(15));

        rightBar.getChildren().addAll(
                toolsHeader,
                shareBtn,
                undoBtn,
                redoBtn,
                separator,
                metadataHeader,
                docMetadataBox,
                new Separator(),
                usersHeader,
                activeUsersBox
        );
        rightBar.setPrefWidth(220);

        root.setTop(sessionBar);
        root.setCenter(centerBox);
        root.setRight(rightBar);
    }

    private void setupEventHandlers() {

        backBtn.setOnAction(e -> {
            if (stompClient != null) {
                stompClient.disconnect();
            }
            DocumentsOverviewController overviewController = new DocumentsOverviewController();
            root.getScene().setRoot(overviewController.getRoot());
        });
        System.out.println("Not Null");
        shareBtn.setOnAction(e -> showShareDialog());

        textArea.textProperty().addListener((obs, oldText, newText) -> {
            if (stompClient == null || !textArea.isEditable() || isApplyingRemoteUpdate) return;  // Skip if flag is true

            int changePos = findChangePosition(oldText, newText);
            if (changePos >= 0) {
                if (newText.length() > oldText.length()) {
                    char c = newText.charAt(changePos);
                    System.out.println(c);
                    System.out.println("is inserted");
                    InsertRequest req = new InsertRequest(userId, c, changePos);
                    sendMessage("/app/document/" + docId + "/insert", req);
                } else if (oldText.length() > newText.length()) {
                    DeleteRequest req = new DeleteRequest(userId, changePos);
                    System.out.println("is deleted");
                    sendMessage("/app/document/" + docId + "/delete", req);
                }
            }
        });


        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (stompClient == null || !textArea.isEditable()) return;
            CursorUpdateRequest req = new CursorUpdateRequest(userId, newPos.intValue(), getColorForUser(userId));
            sendMessage("/app/document/" + docId + "/cursor", req);
        });

        undoBtn.setOnAction(e -> {
            if (stompClient == null) return;
            sendMessage("/app/document/" + docId + "/undo", new UndoRequest(userId));
        });

        redoBtn.setOnAction(e -> {
            if (stompClient == null) return;
            sendMessage("/app/document/" + docId + "/redo", new RedoRequest(userId));
        });

        // Add hover effects for buttons
        setupButtonHoverEffect(shareBtn, "#28A745", "#218838");
        setupButtonHoverEffect(undoBtn, "#007BFF", "#0069D9");
        setupButtonHoverEffect(redoBtn, "#007BFF", "#0069D9");
        setupButtonHoverEffect(backBtn, "#6C757D", "#5A6268");
    }

    private void setupButtonHoverEffect(Button button, String baseColor, String hoverColor) {
        button.setOnMouseEntered(e -> {
            String style = button.getStyle().replace(baseColor, hoverColor);
            button.setStyle(style);
        });

        button.setOnMouseExited(e -> {
            String style = button.getStyle().replace(hoverColor, baseColor);
            button.setStyle(style);
        });
    }

    private void loadDocument() {
        System.out.println("[DEBUG] loadDocument() called");
        loadingIndicator.setVisible(true);
        statusLabel.setText("Fetching document from server...");

        String apiUrl = API_URL + "documents/" + docId;
        System.out.println("[DEBUG] Constructed API URL: " + apiUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        System.out.println("[DEBUG] HTTP GET request built, sending asynchronously...");

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("[DEBUG] Received HTTP response with status code: " + response.statusCode());
                    System.out.println("[DEBUG] Response body:\n" + response.body());
                    handleDocumentResponse(response.body());
                })
                .exceptionally(e -> {
                    System.out.println("[ERROR] HTTP request failed: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        statusLabel.setText("Error: Failed to load document");
                        statusLabel.setStyle("-fx-text-fill: #DC3545;");

                        showErrorAlert("Failed to load document",
                                "Could not retrieve the document from the server.",
                                e.getMessage());
                    });
                    return null;
                });
    }


    private void handleDocumentResponse(String jsonResponse) {
        System.out.println("[DEBUG] Received JSON response:\n" + jsonResponse);

        try {
            // Parse the document response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(jsonResponse);
            DocumentResponse docResponse = new DocumentResponse(
                    json.get("id").asText(),
                    json.get("title").asText(),
                    json.get("ownerId").asText(),
                    json.get("createdAt").asText(),
                    json.get("updatedAt").asText(),
                    json.get("editorCode").asText(),
                    json.get("viewerCode").asText(),
                    json.get("status").asText(),
                    new DocumentResponse.CrdtDocument(
                            json.get("crdtDocument").get("activeUsers").asText(),
                            json.get("crdtDocument").get("text").asText(),
                            mapper.convertValue(json.get("crdtDocument").get("allCursors"),
                                    new TypeReference<List<CursorPosition>>() {})
                    )
            );
            this.editorCode = json.get("editorCode").asText();
            this.viewerCode = json.get("viewerCode").asText();

            System.out.println("====== DocumentResponse Debug Info ======");
            System.out.println("ID: " + json.get("id").asText());
            System.out.println("Title: " + json.get("title").asText());
            System.out.println("Owner ID: " + json.get("ownerId").asText());
            System.out.println("Created At: " + json.get("createdAt").asText());
            System.out.println("Updated At: " + json.get("updatedAt").asText());
            System.out.println("Editor Code: " + json.get("editorCode").asText());
            System.out.println("Viewer Code: " + json.get("viewerCode").asText());
            System.out.println("Status: " + json.get("status").asText());

            JsonNode crdtNode = json.get("crdtDocument");
            System.out.println("---- CRDT Document ----");
            System.out.println("Active Users: " + crdtNode.get("activeUsers").asText());
            System.out.println("Text: " + crdtNode.get("text").asText());


            System.out.println("All Cursors:");
            for (JsonNode cursorNode : crdtNode.get("allCursors")) {
                System.out.println("  Cursor => userId: " + cursorNode.get("userId").asText() +
                        ", position: " + cursorNode.get("position").asInt());
            }
            System.out.println("=========================================");
            Platform.runLater(() -> {
                System.out.println("[DEBUG] Updating UI on JavaFX thread");

                // Update text area with document content
                textArea.setText(docResponse.getCrdtDocument().getText());

                // Update document metadata
                documentTitle = docResponse.getTitle();
                documentCreator = docResponse.getOwnerId();
                lastModified = docResponse.getUpdatedAt();

                // Update UI with document info
                docInfoLabel.setText(documentTitle);

                VBox docMetadataBox = (VBox) rightBar.getChildren().get(6);
                ((Label) docMetadataBox.getChildren().get(0)).setText(documentTitle);
                ((Label) docMetadataBox.getChildren().get(1)).setText("Creator: " + documentCreator);
                ((Label) docMetadataBox.getChildren().get(2)).setText("Modified: " + lastModified);

                // Enable editing if in editor mode
                System.out.println("I am an editor? " + isEditor);
                textArea.setEditable(this.isEditor);

                // Hide loading indicator
                loadingIndicator.setVisible(false);
                statusLabel.setText("Document loaded successfully");
                if (docResponse.getCrdtDocument().getAllCursors() != null) {
                    // Clear existing data
                    remoteCursors.clear();
                    cursorColors.clear();
                    activeUsers.clear();

                    // Process each cursor from the response
                    for (CursorPosition cursor : docResponse.getCrdtDocument().getAllCursors()) {
                        String userId = cursor.getUserId();
                        remoteCursors.put(userId, cursor.getPosition());
                        cursorColors.put(userId, cursor.getColor());
                        updateActiveUsers(userId, cursor.getColor());
                    }

                    // Visualize the cursors
                    visualizeRemoteCursors();
                }

                // Connect to WebSocket for real-time collaboration
                System.out.println("[DEBUG] Attempting to connect to session...");
                connectToSession();
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to parse document response: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                statusLabel.setText("Error: Failed to parse document data");
                statusLabel.setStyle("-fx-text-fill: #DC3545;");

                showErrorAlert("Document Format Error",
                        "The document data could not be properly parsed.",
                        e.getMessage());
            });
        }
    }


    private void showShareDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Share Document");
        dialog.setHeaderText("Share this document with others");

        // Generate sharing codes
        String editorCode = this.editorCode;
        String viewerCode = this.viewerCode;

        // Create dialog content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #F8F9FA;");

        Label introLabel = new Label("Share these codes with collaborators:");
        introLabel.setStyle("-fx-font-size: 14px;");

        // Editor code section
        VBox editorBox = new VBox(5);
        Label editorLabel = new Label("Editor Access Code");
        editorLabel.setStyle("-fx-font-weight: bold;");

        TextField editorField = new TextField(editorCode);
        editorField.setEditable(false);
        editorField.setPrefWidth(300);
        editorField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #DEE2E6;" +
                        "-fx-border-radius: 3px;"
        );

        Button copyEditorBtn = new Button("Copy");
        copyEditorBtn.setStyle(
                "-fx-background-color: #6C757D;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-cursor: hand;"
        );
        copyEditorBtn.setOnAction(e -> {
            editorField.selectAll();
            editorField.copy();
            copyEditorBtn.setText("✓ Copied!");
        });

        HBox editorRow = new HBox(10, editorField, copyEditorBtn);
        editorBox.getChildren().addAll(editorLabel, editorRow);

        // Viewer code section
        VBox viewerBox = new VBox(5);
        Label viewerLabel = new Label("Viewer Access Code");
        viewerLabel.setStyle("-fx-font-weight: bold;");

        TextField viewerField = new TextField(viewerCode);
        viewerField.setEditable(false);
        viewerField.setPrefWidth(300);
        viewerField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #DEE2E6;" +
                        "-fx-border-radius: 3px;"
        );

        Button copyViewerBtn = new Button("Copy");
        copyViewerBtn.setStyle(
                "-fx-background-color: #6C757D;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-cursor: hand;"
        );
        copyViewerBtn.setOnAction(e -> {
            viewerField.selectAll();
            viewerField.copy();
            copyViewerBtn.setText("✓ Copied!");
        });

        HBox viewerRow = new HBox(10, viewerField, copyViewerBtn);
        viewerBox.getChildren().addAll(viewerLabel, viewerRow);

        // Note section
        Label noteLabel = new Label("Note: Anyone with these codes can join your document session.");
        noteLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #6C757D;");

        content.getChildren().addAll(
                introLabel,
                editorBox,
                viewerBox,
                new Separator(),
                noteLabel
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(450);

        dialog.showAndWait();
    }

    // In your CollabSessionController
    private void connectToSession() {
        try {
            String wsUrl = "ws://localhost:8080/ws";
            stompClient = new CollaborationStompClient(
                    wsUrl,
                    docId,  // Pass document ID
                    this::handleServerMessage,
                    () -> updateConnectionStatus(false)
            );

            this.messageHandler = this::handleServerMessage;
            stompClient.connect();

            // Send initial connection message
            ConnectRequest joinReq = new ConnectRequest(userId, docId);
            stompClient.send("/app/document/connect", joinReq);

            updateConnectionStatus(true);
        } catch (Exception e) {
            Platform.runLater(() -> {
                statusLabel.setText("Connection failed: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: #DC3545;");
            });
            e.printStackTrace();
        }
    }

    private void sendMessage(String destination, Object body) {
        if (stompClient != null) {
            try {
                stompClient.send(destination, body); // STOMP handles JSON conversion
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Send error: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #DC3545;");
                });
            }
        }
    }

    private void handleServerMessage(String msg) {
        try {
            if (msg.contains("\"operationType\"")) {
                DocumentStateResponse resp = objectMapper.readValue(msg, DocumentStateResponse.class);
                Platform.runLater(() -> {
                    if (!isApplyingRemoteUpdate && !textArea.getText().equals(resp.getText())) {
                        isApplyingRemoteUpdate = true; // Prevent triggering the listener

                        textArea.setText(resp.getText());

                        isApplyingRemoteUpdate = false; // Re-enable the listener
                    }
                    visualizeRemoteCursors();
                });
            } else if (msg.contains("\"userId\"") && msg.contains("\"position\"")) {
                CursorResponse resp = objectMapper.readValue(msg, CursorResponse.class);
                Platform.runLater(() -> {
                    remoteCursors.put(resp.getUserId(), resp.getPosition());
                    cursorColors.put(resp.getUserId(), resp.getColor());
                    updateActiveUsers(resp.getUserId(), resp.getColor());
                    visualizeRemoteCursors();
                });
            } else if (msg.contains("\"users\"")) {
                // Handle user list updates if server sends them
                UserListResponse resp = objectMapper.readValue(msg, UserListResponse.class);
                Platform.runLater(() -> {
                    resp.getUsers().forEach(user -> {
                        updateActiveUsers(user.getUserId(), user.getColor());
                    });
                });
            }
        } catch (Exception e) {
            System.err.println("Error processing server message: " + e.getMessage());
        }
    }


    private void updateActiveUsers(String userId, String color) {
        // Store user in our map
        activeUsers.put(userId, color);

        // Refresh the active users display
        Platform.runLater(() -> {
            activeUsersBox.getChildren().clear();

            activeUsers.forEach((id, clr) -> {
                HBox userRow = new HBox(10);
                userRow.setAlignment(Pos.CENTER_LEFT);

                // Color indicator
                Region colorIndicator = new Region();
                colorIndicator.setPrefSize(12, 12);
                colorIndicator.setMinSize(12, 12);
                colorIndicator.setMaxSize(12, 12);
                colorIndicator.setStyle("-fx-background-color: " + clr + "; -fx-background-radius: 6px;");

                // Username label (show "You" for current user)
                String displayName = id.equals(this.userId) ? "You" : "User " + id.substring(0, 4);
                Label nameLabel = new Label(displayName);
                if (id.equals(this.userId)) {
                    nameLabel.setStyle("-fx-font-weight: bold;");
                }

                userRow.getChildren().addAll(colorIndicator, nameLabel);
                activeUsersBox.getChildren().add(userRow);
            });
        });
    }

    private void visualizeRemoteCursors() {
        // Note: This is a simplified version, for better cursor visualization
        // consider using a rich text component like RichTextFX
        StringBuilder styleBuilder = new StringBuilder();
        styleBuilder.append("-fx-highlight-fill: #FFFACD;"); // Default highlight color

        for (Map.Entry<String, Integer> entry : remoteCursors.entrySet()) {
            if (!entry.getKey().equals(userId)) {
                String color = cursorColors.getOrDefault(entry.getKey(), "#FF0000");
                int position = entry.getValue();

                // This is a simplified approach to visualize cursors
                // A more advanced implementation would use custom text highlighting
                styleBuilder.append(String.format(
                        "-fx-background-color: %s33; ", // Add transparency
                        color
                ));
            }
        }

        textArea.setStyle(styleBuilder.toString());
    }

    private void updateConnectionStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                statusLabel.setText("Connected to document");
                statusLabel.setStyle("-fx-text-fill: #28A745;");
                loadingIndicator.setVisible(false);
            } else {
                statusLabel.setText("Disconnected");
                statusLabel.setStyle("-fx-text-fill: #DC3545;");
                showErrorAlert("Connection Lost",
                        "You have been disconnected from the collaboration session.",
                        "Try reloading the document or check your internet connection.");
            }
        });
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: #F8F9FA;" +
                        "-fx-border-color: #DC3545;" +
                        "-fx-border-width: 2px;"
        );

        alert.showAndWait();
    }

    private int findChangePosition(String oldText, String newText) {
        int min = Math.min(oldText.length(), newText.length());
        for (int i = 0; i < min; i++) {
            if (oldText.charAt(i) != newText.charAt(i)) return i;
        }
        return newText.length() > oldText.length() ? newText.length() - 1 : min;
    }

    private String getColorForUser(String userId) {
        int code = userId.hashCode();
        Color color = Color.hsb((code % 360 + 360) % 360, 0.7, 0.8);
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }

    public BorderPane getRoot() {
        return root;
    }

    public boolean isApplyingRemoteUpdate() {
        return isApplyingRemoteUpdate;
    }

    public void setApplyingRemoteUpdate(boolean applyingRemoteUpdate) {
        isApplyingRemoteUpdate = applyingRemoteUpdate;
    }


    private static class UserListResponse {
        private java.util.List<UserInfo> users;

        public java.util.List<UserInfo> getUsers() { return users; }
        public void setUsers(java.util.List<UserInfo> users) { this.users = users; }
    }

    private static class UserInfo {
        private String userId;
        private String color;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
}