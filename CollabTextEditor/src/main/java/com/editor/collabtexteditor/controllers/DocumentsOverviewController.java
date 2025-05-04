package com.editor.collabtexteditor.controllers;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class DocumentsOverviewController {
    private final BorderPane root = new BorderPane();
    private final String generatedUserId = UUID.randomUUID().toString().substring(0, 8);

    public DocumentsOverviewController() {
        initializeUI();
        fetchDocuments();
    }

    private void initializeUI() {
        // Header
        Label titleLabel = new Label("My Documents");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button createBtn = new Button("Create New Document");
        createBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white;");

        HBox header = new HBox(10, titleLabel, createBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));

        // Document Grid
        TilePane documentsGrid = new TilePane();
        documentsGrid.setPadding(new Insets(20));
        documentsGrid.setHgap(20);
        documentsGrid.setVgap(20);
        documentsGrid.setPrefColumns(3);

        ScrollPane scrollPane = new ScrollPane(documentsGrid);
        scrollPane.setFitToWidth(true);

        root.setTop(header);
        root.setCenter(scrollPane);

        // Event Handlers
        createBtn.setOnAction(e -> showCreateDocumentDialog());
    }

    private void fetchDocuments() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/documents"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        parseAndAddDocumentTitles(response);
                    });
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private void parseAndAddDocumentTitles(String response) {
        // Assuming the response is a JSON array of documents, each with a "title" field
        try {
            JSONArray jsonArray = new JSONArray(response);
            ObservableList<String> documentTitles = FXCollections.observableArrayList();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject document = jsonArray.getJSONObject(i);
                String title = document.getString("title");
                documentTitles.add(title);
            }

            // Now, use documentTitles to update your UI, e.g., adding them to a ListView
            documentListView.setItems(documentTitles);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void addDocumentCard(String docName) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 5;");

        Rectangle thumbnail = new Rectangle(150, 100);
        thumbnail.setFill(Color.LIGHTGRAY);

        Label nameLabel = new Label(docName);
        nameLabel.setStyle("-fx-font-weight: bold;");

        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> showJoinOptions(docName));

        card.getChildren().addAll(thumbnail, nameLabel, openBtn);
        ((TilePane) ((ScrollPane) root.getCenter()).getContent()).getChildren().add(card);
    }

    private void showJoinOptions(String docName) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Join Document");
        dialog.setHeaderText("Join " + docName);

        // Create radio buttons for join options
        ToggleGroup group = new ToggleGroup();
        RadioButton editorBtn = new RadioButton("Join as Editor");
        RadioButton viewerBtn = new RadioButton("Join as Viewer");
        editorBtn.setToggleGroup(group);
        viewerBtn.setToggleGroup(group);
        editorBtn.setSelected(true);

        // Code input field
        TextField codeField = new TextField();
        codeField.setPromptText("Enter access code");

        VBox content = new VBox(15,
                new Label("Select your access level:"),
                editorBtn,
                viewerBtn,
                new Label("Enter access code:"),
                codeField);
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String mode = editorBtn.isSelected() ? "editor" : "viewer";
                return mode + ":" + codeField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String[] parts = result.split(":");
            String mode = parts[0];
            String code = parts[1];

            // Validate code and open document
            openDocument(docName, code, mode);
        });
    }

    private void showCreateDocumentDialog() {
        // Create the document via API
        // POST http://localhost:8080/api/documents

        // For demo, we'll just create a local document
        String newDocId = "DOC-" + UUID.randomUUID().toString().substring(0, 5);
        openDocument("New Document", newDocId, "editor");
    }

    private void openDocument(String docName, String docId, String mode) {
        CollabSessionController sessionController = new CollabSessionController(docId, generatedUserId, mode);
        root.getScene().setRoot(sessionController.getRoot());
    }

    public BorderPane getRoot() {
        return root;
    }
}