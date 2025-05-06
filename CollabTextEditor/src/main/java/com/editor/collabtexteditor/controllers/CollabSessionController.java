package com.editor.collabtexteditor.controllers;

import com.editor.collabtexteditor.Networking.CollaborationStompClient;
import com.editor.collabtexteditor.model.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.editor.collabtexteditor.Configs.API_URL;

public class CollabSessionController {
    @Getter
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

    private final Button exportBtn = new Button("Export");
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
    private boolean isApplyingRemoteCursor = false;
    private final Pane overlayPane = new Pane();
    private final Map<String, Rectangle> cursorIndicators = new ConcurrentHashMap<>();
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
    Rectangle test = new Rectangle(2, 18, Color.RED);


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

        exportBtn.setStyle(buttonStyle.replace("#007BFF", "#FFC107")); // Amber


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

                exportBtn,
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

        overlayPane.setMouseTransparent(true); // Allow clicks to pass through
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.prefWidthProperty().bind(textArea.widthProperty());
        overlayPane.prefHeightProperty().bind(textArea.heightProperty());

        // Add to root (make sure textArea is added first)
        root.getChildren().addAll(textArea, overlayPane);
    }

    private void setupEventHandlers() {

        backBtn.setOnAction(e -> {
            if (stompClient != null) {
                ConnectRequest req = new ConnectRequest(userId, isEditor);
                System.out.println("Sending disconnect request: " + req);
                stompClient.safeDisconnect("/app/document/" + docId + "/disconnect",req);
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
            if (stompClient == null || !textArea.isEditable() || isApplyingRemoteCursor) return;

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



        exportBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Document");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            fileChooser.setInitialFileName("document_" + docId + ".txt");

            File file = fileChooser.showSaveDialog(root.getScene().getWindow());

            if (file != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(textArea.getText());  // Get text directly from TextArea
                    statusLabel.setText("Export successful.");
                    statusLabel.setStyle("-fx-text-fill: green;");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    statusLabel.setText("Export failed.");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            }
        });


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

    private static HttpRequest.BodyPublisher ofFileMultipart(File file) throws IOException {
        String boundary = "---011000010111000001101001";
        var byteArrays = new ArrayList<byte[]>();

        byteArrays.add(("--" + boundary + "\r\n").getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
        byteArrays.add(("Content-Type: text/plain\r\n\r\n").getBytes());
        byteArrays.add(Files.readAllBytes(file.toPath()));
        byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
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

                VBox docMetadataBox = (VBox) rightBar.getChildren().get(7);
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
                        updateActiveUsers(userId, cursor.getColor(),true);
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

            ConnectRequest joinReq = new ConnectRequest(userId, isEditor);
            stompClient.send("/app/document/"+docId+"/connect", joinReq);

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
            System.out.println("[WebSocket] Received server message: " + msg);
            if (msg.contains("\"operationType\"")) {
                DocumentStateResponse resp = objectMapper.readValue(msg, DocumentStateResponse.class);
                Platform.runLater(() -> {
                    if (!isApplyingRemoteUpdate && !textArea.getText().equals(resp.getText())) {
                        isApplyingRemoteUpdate = true; // Prevent triggering the listener
                        int caretPosition = textArea.getCaretPosition();
                        textArea.setText(resp.getText());
                        // Restore cursor position if possible
                        if (caretPosition <= resp.getText().length()) {
                            textArea.positionCaret(caretPosition);
                        } else {
                            // If the document is now shorter, place cursor at the end
                            textArea.positionCaret(resp.getText().length());
                        }

                        isApplyingRemoteUpdate = false; // Re-enable the listener
                    }
                    visualizeRemoteCursors();
                });
            }else if (msg.contains("\"userId\"") && msg.contains("\"position\"")) {
                CursorResponse resp = objectMapper.readValue(msg, CursorResponse.class);
                System.out.println("[CURSOR] Received cursor update:");
                System.out.println("  User ID: " + resp.getUserId());
                System.out.println("  Position: " + resp.getPosition());
                System.out.println("  Color: " + resp.getColor());
                System.out.println("  Is current user? " + resp.getUserId().equals(userId));

                Platform.runLater(() -> {
                    if (!resp.getUserId().equals(userId)) {
                        System.out.println("[CURSOR] Storing remote cursor:");
                        System.out.println("  Adding to remoteCursors: " + resp.getUserId() + " -> " + resp.getPosition());
                        System.out.println("  Adding to cursorColors: " + resp.getUserId() + " -> " + resp.getColor());

                        remoteCursors.put(resp.getUserId(), resp.getPosition());
                        cursorColors.put(resp.getUserId(), resp.getColor());
                        updateActiveUsers(resp.getUserId(), resp.getColor(), true);
                        visualizeRemoteCursors();
                    } else {
                        System.out.println("[CURSOR] Ignoring own cursor update");
                    }
                });
            } else if (msg.contains("\"eventType\"")) {
                UserConnWectionEvent event = objectMapper.readValue(msg, UserConnWectionEvent.class);
                ConnectRequest request = event.getRequest();
                String eventType = event.getEventType();
                Platform.runLater(() -> {
                    if ("user-joined".equals(eventType)) {

                        String color = getColorForUser(request.getUserId());
                        updateActiveUsers(request.getUserId(), color,true);
                    } else if ("user-left".equals(eventType)) {
                        String color = getColorForUser(request.getUserId());
                        updateActiveUsers(request.getUserId(),color,false);
                        remoteCursors.remove(request.getUserId());
                        cursorColors.remove(request.getUserId());

                        if (remoteCursors.remove(userId) != null) {
                            System.out.println("  ✔ Removed from remoteCursors");
                        } else {
                            System.out.println("  ✘ Not found in remoteCursors");
                        }

                        if (cursorColors.remove(userId) != null) {
                            System.out.println("  ✔ Removed from cursorColors");
                        } else {
                            System.out.println("  ✘ Not found in cursorColors");
                        }

                        Rectangle indicator = cursorIndicators.remove(userId);
                        if (indicator != null) {
                            overlayPane.getChildren().remove(indicator);
                            System.out.println("  ✔ Cursor indicator removed from overlay");
                        } else {
                            System.out.println("  ✘ No cursor indicator found in cursorIndicators");
                        }
                        visualizeRemoteCursors();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error processing server message: " + e.getMessage());
        }
    }


    private void updateActiveUsers(String userId, String color,boolean addOrRemove) {
        if(addOrRemove)
            activeUsers.put(userId, color);
        else
            activeUsers.remove(userId, color);

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

                String displayName = id.equals(this.userId) ? "You" : "User " + id.substring(0, 3);
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

        // remove indicators of users that are gone

        Iterator<String> it = cursorIndicators.keySet().iterator();
        while (it.hasNext()) {
            String id = it.next();
            if (!remoteCursors.containsKey(id)) {
                Rectangle indicator = cursorIndicators.get(id);
                if (indicator != null) {
                    // Make it visually disappear
                    indicator.setWidth(0);
                    indicator.setHeight(0);
                    // Optionally, remove it from the overlayPane now or later
                    overlayPane.getChildren().remove(indicator);
                }
                it.remove(); // Now remove from the map
            }
        }


        for (Map.Entry<String,Integer> e : remoteCursors.entrySet()) {

            String uid   = e.getKey();
            int    pos   = e.getValue();

            if (uid.equals(this.userId)) continue; // skip own caret

            Point2D loc = estimateCursorPosition(pos);

            Rectangle r = cursorIndicators.computeIfAbsent(uid, k -> {
                Rectangle rect = new Rectangle(2, computeLineHeight(textArea.getFont()));
                rect.setMouseTransparent(true);
                overlayPane.getChildren().add(rect);
                return rect;
            });

            r.setFill(Color.web(cursorColors.getOrDefault(uid,"#ff0000")));
            r.setLayoutX(loc.getX());
            r.setLayoutY(loc.getY());
        }
    }

    private Point2D estimateCursorPosition(int index) {

        String txt = textArea.getText();
        index = Math.min(index, txt.length());

        Font   font       = textArea.getFont();
        double charW      = computeAverageCharWidth(font);
        double lineH      = computeLineHeight(font);

        // How many “columns” fit into the current visible width?
        Insets padding    = textArea.getPadding();
        double usableW    = textArea.getWidth()
                - padding.getLeft() - padding.getRight() - 2; // 2px safety
        int maxColsPerRow = (int) Math.max(1, Math.floor(usableW / charW)) -1;

        int row = 0;
        int col = 1;

    /* Walk through the text once up to the requested index and do a
       manual word-wrap counting.  That is fast enough for typical
       document sizes (< 100 k). */
        for (int i = 0; i < index; i++) {
            char c = txt.charAt(i);

            if (c == '\n') {          // explicit line break
                row++;                // new physical line
                col = 1;
            } else {
                col++;
                if (col >= maxColsPerRow) {   // automatic wrap
                    row++;
                    col = 1;
                }
            }
        }

        double localX = padding.getLeft() + col * charW;
        double localY = padding.getTop()  + row * lineH+6;

        Point2D pScene  = textArea.localToScene(localX, localY);
        return overlayPane.sceneToLocal(pScene);
    }


    private double computeLineHeight(Font font) {
        Text t = new Text("X");
        t.setFont(font);
        return t.getLayoutBounds().getHeight()+4.8;
    }


    private double computeAverageCharWidth(Font font) {
        Text t = new Text("X");
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
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
        // Find first differing character from start
        int minLength = Math.min(oldText.length(), newText.length());
        for (int i = 0; i < minLength; i++) {
            if (oldText.charAt(i) != newText.charAt(i)) {
                return i;
            }
        }


        if (newText.length() > oldText.length()) {

            return oldText.length();
        } else if (oldText.length() > newText.length()) {
            // Deletion at end
            return newText.length();
        }


        return -1;
    }

    private String getColorForUser(String userId) {
        int code = userId.hashCode();
        Color color = Color.hsb((code % 360 + 360) % 360, 0.7, 0.8);
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }

    public boolean isApplyingRemoteUpdate() {
        return isApplyingRemoteUpdate;
    }

    public void setApplyingRemoteUpdate(boolean applyingRemoteUpdate) {
        isApplyingRemoteUpdate = applyingRemoteUpdate;
    }

    public boolean isApplyingRemoteCursor() {
        return isApplyingRemoteCursor;
    }

    public void setApplyingRemoteCursor(boolean applyingRemoteCursor) {
        isApplyingRemoteCursor = applyingRemoteCursor;
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