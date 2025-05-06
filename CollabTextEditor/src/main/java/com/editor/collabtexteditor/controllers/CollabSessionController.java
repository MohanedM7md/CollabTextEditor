package com.editor.collabtexteditor.controllers;

import com.editor.collabtexteditor.Networking.CollaborationStompClient;
import com.editor.collabtexteditor.dto.request.*;
import com.editor.collabtexteditor.dto.response.*;
import com.editor.collabtexteditor.model.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
    private final Button commentBtn = new Button("💬 Comment");
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
    private Label docTitleLabel;
    private Label docCreatorLabel;
    private Label docModifiedLabel;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Document metadata
    private String documentTitle = "";
    private String documentCreator = "";
    private String lastModified = "";
    private String viewerCode = "";
    private String editorCode = "";
    /* ========= comments ========= */
    private final Map<String, CommentPosition> comments = new ConcurrentHashMap<>();

    /* UI containers */
    private final VBox commentsBox = new VBox(6);       // right bar list
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

        /* ---------- SESSION BAR (top) -------------------------------- */
        shareBtn.setText("📤 Share");
        undoBtn.setText("↩ Undo");
        redoBtn.setText("↪ Redo");
        HBox.setHgrow(docInfoLabel, Priority.ALWAYS);
        docInfoLabel.setText("Document: " + docId);
        HBox statusBox = new HBox(10, loadingIndicator, statusLabel);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        sessionBar.getChildren().addAll(backBtn, docInfoLabel, shareBtn,
                undoBtn,
                redoBtn,exportBtn, statusBox);
        sessionBar.setAlignment(Pos.CENTER_LEFT);

        /* ---------- TEXT AREA (center) -------------------------------- */
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefHeight(500);
        ScrollPane scroll = new ScrollPane(textArea);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        VBox centerBox = new VBox(10, scroll, statusLabel);
        centerBox.setPadding(new Insets(15));

        /* ---------- RIGHT SIDEBAR ------------------------------------ */
        rightBar.setPrefWidth(220);

        /* 1) TOOLS ----------------------------------------------------- */
        Label toolsHeader = header("DOCUMENT TOOLS");



        rightBar.getChildren().addAll(
                toolsHeader,
                commentBtn,          // ← NEW
                new Separator()
        );

        /* 2) DOCUMENT INFO -------------------------------------------- */
        Label infoHeader = header("DOCUMENT INFO");

        docTitleLabel    = new Label("Loading…"); docTitleLabel.setStyle("-fx-font-weight:bold;");
        docCreatorLabel  = new Label("Creator: Loading…");
        docModifiedLabel = new Label("Last modified: Loading…");

        VBox docBox = boxed(docTitleLabel, docCreatorLabel, docModifiedLabel);

        rightBar.getChildren().addAll(
                infoHeader,
                docBox,
                new Separator()
        );

        /* 3) ACTIVE USERS --------------------------------------------- */
        Label usersHeader = header("ACTIVE USERS");
        rightBar.getChildren().addAll(usersHeader, activeUsersBox, new Separator());

        /* 4) COMMENTS -------------------------------------------------- */
        Label commentsHeader = header("COMMENTS");
        rightBar.getChildren().addAll(commentsHeader, commentsBox);

        /* ---------- put panes in BorderPane --------------------------- */
        root.setTop(sessionBar);
        root.setCenter(centerBox);
        root.setRight(rightBar);

        /* overlay for remote cursors / comments */
        overlayPane.setMouseTransparent(true);
        overlayPane.prefWidthProperty().bind(textArea.widthProperty());
        overlayPane.prefHeightProperty().bind(textArea.heightProperty());
        root.getChildren().addAll(textArea, overlayPane);
    }

    /* small helper to create section headers */
    private Label header(String text) {
        Label h = new Label(text);
        h.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6C757D;");
        return h;
    }

    /* helper to build a white boxed container */
    private VBox boxed(Node... nodes) {
        VBox b = new VBox(5, nodes);
        b.setStyle("-fx-background-color:white;-fx-padding:10px;"
                + "-fx-border-color:#DEE2E6;-fx-border-radius:5px;");
        return b;
    }

    private void setupEventHandlers() {
        commentBtn.setOnAction(e -> {
            IndexRange sel = textArea.getSelection();
            if (sel.getLength() == 0) {
                statusLabel.setText("Select some text first");
                statusLabel.setStyle("-fx-text-fill: orange;");
                return;
            }

            // Create comment input dialog
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Add Comment");
            dialog.setHeaderText("Write your comment for the selected text");

            // Set up buttons
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

            // Create text area for comment input
            TextArea commentInput = new TextArea();
            commentInput.setPromptText("Enter your comment here...");
            commentInput.setWrapText(true);
            commentInput.setPrefRowCount(4);

            dialog.getDialogPane().setContent(commentInput);

            // Convert result to string when OK is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButton) {
                    return commentInput.getText();
                }
                return null;
            });

            // Show dialog and handle result
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(commentText -> {
                Color pick = Color.web(getColorForUser(userId));
                String hex = String.format("#%02X%02X%02X",
                        (int)(pick.getRed()*255),
                        (int)(pick.getGreen()*255),
                        (int)(pick.getBlue()*255));

                AddCommentRequest req = new AddCommentRequest(
                        userId,
                        sel.getStart(),
                        sel.getEnd(),
                        hex,
                        commentText  // Add the comment text to the request
                );

                sendMessage("/app/document/" + docId + "/comment/add", req);
            });
        });
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

        textArea.textProperty().addListener((_, oldText, newText) -> {
            if (stompClient == null || !textArea.isEditable() || isApplyingRemoteUpdate) return;

            int changePos = findChangePosition(oldText, newText);
            if (changePos >= 0) {
                int delta = newText.length() - oldText.length();

                if (delta > 0) {
                    String inserted = newText.substring(changePos, changePos + delta);
                    if (delta == 1) {
                        // Single character insert
                        InsertRequest req = new InsertRequest(userId, inserted.charAt(0), changePos);
                        sendMessage("/app/document/" + docId + "/insert", req);
                    } else {
                        // Bulk insert
                        System.out.println("Inserted " + inserted);
                        BulkInsertRequest bulkReq = new BulkInsertRequest(userId, inserted, changePos);
                        sendMessage("/app/document/" + docId + "/insert/bulk", bulkReq);
                    }
                } else if (delta < 0) {
                    // Deletion handling remains the same
                    int deleteCount = -delta;
                    for (int i = 0; i < deleteCount; i++) {
                        DeleteRequest req = new DeleteRequest(userId, changePos);
                        sendMessage("/app/document/" + docId + "/delete", req);
                    }
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
                                    new TypeReference<List<CursorPosition>>() {}),
                            mapper.convertValue(json.get("crdtDocument").get("comments"),
                                    new TypeReference<List<CommentPosition>>() {})
                    )
            );
            this.editorCode = json.get("editorCode").asText();
            this.viewerCode = json.get("viewerCode").asText();
            JsonNode crdtNode = json.get("crdtDocument");
            Platform.runLater(() -> {
                System.out.println("[DEBUG] Updating UI on JavaFX thread");

                // Update text area with document content
                textArea.setText(docResponse.getCrdtDocument().getText());

                // Update document metadata
                docTitleLabel.setText(documentTitle);
                docCreatorLabel.setText("Creator: " + documentCreator);
                docModifiedLabel.setText("Modified: " + lastModified);;

                // Update UI with document info
                docInfoLabel.setText(documentTitle);

                VBox docMetadataBox = (VBox) rightBar.getChildren().get(4);
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

                    // Process eacht cursor from the response
                    for (CursorPosition cursor : docResponse.getCrdtDocument().getAllCursors()) {
                        String userId = cursor.getUserId();
                        remoteCursors.put(userId, cursor.getPosition());
                        cursorColors.put(userId, cursor.getColor());
                        updateActiveUsers(userId, cursor.getColor(),true);
                    }

                    // Visualize the cursors
                    visualizeRemoteCursors();
                }
                comments.clear();
                for (CommentPosition cp : docResponse.getCrdtDocument().getAllComments()) {
                    comments.put(cp.getId(), cp);
                }
                updateCommentsUI();
                visualizeComments();
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
                        textArea.positionCaret(Math.min(caretPosition, resp.getText().length()));
                        isApplyingRemoteUpdate = false; // Re-enable the listener
                    }
                    // Update comments from response
                    comments.clear();
                    if (resp.getComments() != null) {
                        for (CommentPosition cp : resp.getComments()) {
                            comments.put(cp.getId(), cp);
                        }
                    }

                    visualizeRemoteCursors();
                    visualizeComments();  // Add this line
                    updateCommentsUI();   // Add this line
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

    /* remove old rectangles (tagged via setUserData) and draw new ones */
    private void visualizeComments() {

        overlayPane.getChildren().removeIf(n -> "comment".equals(n.getUserData()));

        double charW  = computeAverageCharWidth(textArea.getFont());
        double lineH  = computeLineHeight(textArea.getFont());
        Insets pad    = textArea.getPadding();

        for (CommentPosition cp : comments.values()) {

            int start = cp.getStartPos();
            int end   = cp.getEndPos();          // inclusive

            // walk line by line so that multi-line comments get multiple rects
            int current = start;
            while (current < end) {

                int rowStart = current;
                int row = getLineOfPosition(current);

                // how many chars left in this *visual* line?
                int colsPerLine = (int) Math.floor(
                        (textArea.getWidth() - pad.getLeft() - pad.getRight() - 2) / charW);

                int col = (rowStart);
                for (int i = rowStart - 1; i >= 0 && textArea.getText().charAt(i) != '\n'; i--) {
                    col--;                                   // count back to col 0
                }
                int spaceInRow = colsPerLine - col;
                int len = Math.min(spaceInRow, end - current);

                double x = pad.getLeft() + col * charW;
                double y = pad.getTop() + row * lineH - textArea.getScrollTop();
                double w = len * charW;
                double h = lineH;

                Rectangle rect = new Rectangle(w, h, Color.web(cp.getColor(), 0.3));
                if(getLineStart(cp.getStartPos()) != 0)
                    rect.setLayoutX(30 + getLineStart(cp.getStartPos())*charW);
                else
                    rect.setLayoutX(36 + getLineStart(cp.getStartPos())*charW);
                rect.setLayoutY(y+90);
                rect.setMouseTransparent(true);
                rect.setUserData("comment");                 // tag for later removal
                overlayPane.getChildren().add(rect);

                current += len;
                if (len == spaceInRow) current++;            // skip explicit '\n'
            }
        }
    }
    /* refresh the sidebar list of comments */
    private void updateCommentsUI() {
        commentsBox.getChildren().clear();

        for (CommentPosition cp : comments.values()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-radius: 5px; -fx-padding: 5px;");
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f0f0f0;"));
            row.setOnMouseExited(e -> row.setStyle(""));

            // Smaller color indicator
            Region colorDot = new Region();
            colorDot.setPrefSize(8, 8); // Reduced from 12x12
            colorDot.setMinSize(8, 8);
            colorDot.setMaxSize(8, 8);
            colorDot.setStyle("-fx-background-color: " + cp.getColor() + "; -fx-background-radius: 4px;");

            // Comment text
            VBox textBox = new VBox(2);
            Label userLabel = new Label("User " + cp.getUserId());
            userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #444; -fx-font-size: 12px;");

            // Truncated comment preview
            Label commentLabel = new Label(truncateText(cp.getText(), 30));
            commentLabel.setWrapText(true);
            commentLabel.setMaxWidth(200);
            commentLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            textBox.getChildren().addAll(userLabel, commentLabel);

            // Click handler to show detail window
            row.setOnMouseClicked(e -> showCommentDetailDialog(cp));

            row.getChildren().addAll(colorDot, textBox);
            commentsBox.getChildren().add(row);
        }
    }

    private void showCommentDetailDialog(CommentPosition comment) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Comment Details");

        // Create dialog content
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        // Color indicator
        Region colorIndicator = new Region();
        colorIndicator.setPrefSize(16, 16);
        colorIndicator.setStyle("-fx-background-color: " + comment.getColor() + "; -fx-background-radius: 8px;");

        // User info
        HBox userInfo = new HBox(10, colorIndicator, new Label("User: " + comment.getUserId()));
        userInfo.setAlignment(Pos.CENTER_LEFT);

        // Comment text
        TextArea commentText = new TextArea(comment.getText());
        commentText.setEditable(false);
        commentText.setWrapText(true);
        commentText.setPrefRowCount(4);

        // Date/time (if available in your CommentPosition class)
        // Label dateLabel = new Label("Posted: " + comment.getTimestamp());

        content.getChildren().addAll(
                userInfo,
                new Separator(),
                commentText
                // Add dateLabel here if available
        );

        // Close button
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /* helper – count '\n' before index */
    private int getLineOfPosition(int index) {
        String txt = textArea.getText();
        int line = 0;
        for (int i = 0; i < index && i < txt.length(); i++) {
            if (txt.charAt(i) == '\n') line++;
        }
        return line;
    }
    private int getLineStart(int position) {
        String text = textArea.getText();
        System.out.println("getLineStart: " + position);
        int start = 0;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                start = 0;
            }
            start++;
        }
        System.out.println("endStart: " + start);
        return start;
    }


}