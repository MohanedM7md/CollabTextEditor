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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.editor.collabtexteditor.Configs.API_URL;

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
                .uri(URI.create(API_URL+"documents/titles"))
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

        // Code input field
        TextField codeField = new TextField();
        codeField.setPromptText("Enter access code");

        VBox content = new VBox(15,
                new Label("Enter access code:"),
                codeField);
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return codeField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(code -> {
                // Just call the function with name and code
            joinDocumentOnServer(docName, code);
        });
    }
    private void showCreateDocumentDialog() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Create New Document");
        dialog.setHeaderText("Enter document title");

        TextField titleField = new TextField();
        titleField.setPromptText("Document Title");

        // Auto-generate editor and viewer codes
        String editorCode = generateRandomCode();
        String viewerCode = generateRandomCode();

        VBox content = new VBox(10,
                new Label("Owner of the document: " + generatedUserId),
                new Label("Editor Code: " + editorCode),
                new Label("Viewer Code: " + viewerCode),
                new Label("Title:"), titleField);
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                Map<String, String> result = new HashMap<>();
                result.put("ownerId", generatedUserId);
                result.put("editorCode", editorCode);
                result.put("viewerCode", viewerCode);
                result.put("title", titleField.getText());
                return result;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(inputMap -> {
            String ownerId = inputMap.get("ownerId");
            String editor = inputMap.get("editorCode");
            String viewer = inputMap.get("viewerCode");
            String title = inputMap.get("title");

            createDocumentRequest(ownerId, editor, viewer, title);
        });
    }

    private void createDocumentRequest(String ownerId, String editorCode, String viewerCode, String title) {
        String url = API_URL+"documents/create?userId=" + ownerId +
                "&editorcode=" + editorCode + "&viewercode=" + viewerCode + "&title=" + title;
        System.out.println("Creating document for user " + ownerId);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    System.out.println("Create response: " + responseBody);
                    Platform.runLater(() -> {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode json = mapper.readTree(responseBody);
                            String documentId = json.get("id").asText();
                            openDocument(title,documentId, "editor");
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



    private void joinDocumentOnServer(String title, String code) {
        System.out.println("Joining " + title  + " With code: " + code);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL+"documents/by-share-code/" + code+"/"+generatedUserId))
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
                            String mode = json.get("role").asText();
                            System.out.println("Join Doc Id: " + documentId + " Mode: " + mode);
                            openDocument(title,documentId, mode);
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

    private void openDocument(String title,String docId, String mode) {
        System.out.println("Opening " + title  + " With id: " + docId);
        CollabSessionController sessionController = new CollabSessionController(docId,title,
                generatedUserId, mode.equals("editor"));
        root.getScene().setRoot(sessionController.getRoot());
    }

    public BorderPane getRoot() {
        return root;
    }
    private String generateRandomCode() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}