package com.editor.collabtexteditor.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
                .uri(URI.create("http://localhost:8080/api/documents/titles"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    System.out.println("Response from server: " + response);  // Debugging output
                    Platform.runLater(() -> {
                        parseAndAddDocumentTitles(response);
                    });
                })
                .exceptionally(e -> {
                    // Handle the exception and show a user-friendly message
                    handleError(e);
                    return null;
                });
    }

    private void parseAndAddDocumentTitles(String response) {
        try {
            // Parse the response as a JSON array of strings
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonArray = objectMapper.readTree(response);

            ObservableList<String> documentTitles = FXCollections.observableArrayList();

            // Loop through the JSON array and add each string to the documentTitles list
            if (jsonArray.isArray()) {
                for (JsonNode titleNode : jsonArray) {
                    documentTitles.add(titleNode.asText()); // Extract each string value from the JSON array
                }
            }

            // Update UI with document titles
            for (String title : documentTitles) {
                addDocumentCard(title); // Assuming addDocumentCard adds the title to the UI
            }

        } catch (Exception e) {
            // Handle any parsing errors
            handleError(e);
        }
    }
    // Method to handle and display error messages
    private void handleError(Throwable error) {
        String errorMessage = "An error occurred. Please try again later.";

        if (error instanceof java.net.ConnectException) {
            errorMessage = "Failed to connect to the server. Please ensure the server is running.";
        } else if (error instanceof java.net.SocketTimeoutException) {
            errorMessage = "The connection timed out. Please check your network.";
        } else if (error instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            errorMessage = "There was an issue processing the server response. Please try again.";
        }

        // Show an error message in the UI, for example using a dialog
        showErrorDialog(errorMessage);
    }

    // Method to show an error dialog
    private void showErrorDialog(String message) {
        // Using a simple Alert as an example (JavaFX)
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
            joinDocumentOnServer(docName, code, mode);
        });
    }

    private void showCreateDocumentDialog() {
        // Create the document via API
        // POST http://localhost:8080/api/documents

        // For demo, we'll just create a local document
        String newDocId = "DOC-" + UUID.randomUUID().toString().substring(0, 5);
        joinDocumentOnServer("New Document", newDocId, "editor");
    }

    private void joinDocumentOnServer(String name, String mode, String code) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/documents/by-share-code/" + code))
                .GET()
                .build();


        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    System.out.println("Join response: " + responseBody);
                    Platform.runLater(() -> {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode json = mapper.readTree(responseBody);
                            String documentId = json.get("id").asText();
                            openDocument(documentId, mode);
                        } catch (Exception e) {
                            handleError(e);
                        }
                    });
                })
                .exceptionally(e -> {
                    handleError(e);
                    return null;
                });
    }

    private void openDocument(String docId, String mode) {
        CollabSessionController sessionController = new CollabSessionController(docId, generatedUserId, mode);
        root.getScene().setRoot(sessionController.getRoot());
    }

    public BorderPane getRoot() {
        return root;
    }
}