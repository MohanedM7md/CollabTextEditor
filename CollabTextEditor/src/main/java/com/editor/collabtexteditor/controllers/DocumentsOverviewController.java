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
import javafx.stage.FileChooser;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.editor.collabtexteditor.Configs.API_URL;
public class DocumentsOverviewController {
    private final BorderPane root = new BorderPane();
    private final String generatedUserId = UUID.randomUUID().toString().substring(0, 3);
    private final Button importBtn = new Button("Import");

    private static final String BOUNDARY = UUID.randomUUID().toString();
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
        importBtn.setStyle("-fx-background-color: #17A2B8; -fx-text-fill: white;");
        HBox header = new HBox(10, titleLabel, createBtn,importBtn);
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
        importBtn.setOnAction(e -> handleImportDocument());
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
        TextField userNameField = new TextField();
        userNameField.setPromptText("Your name");
        TextField codeField = new TextField();
        codeField.setPromptText("Enter access code");

        VBox content = new VBox(15,
                new Label("Enter a Name:"), userNameField,
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
        final String userId = userNameField.getText() + generatedUserId;
        dialog.showAndWait().ifPresent(code -> {
                // Just call the function with name and code
            joinDocumentOnServer(docName, code,userId);
        });
    }
    private void showCreateDocumentDialog() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Create New Document");
        dialog.setHeaderText("Enter document title");

        TextField titleField = new TextField();
        TextField userNameField = new TextField();
        userNameField.setPromptText("Your name");
        titleField.setPromptText("Document Title");

        // Auto-generate editor and viewer codes
        String editorCode = generateRandomCode();
        String viewerCode = generateRandomCode();

        VBox content = new VBox(10,
                new Label("Enter a Name:"), userNameField,
                new Label("Editor Code: " + editorCode),
                new Label("Viewer Code: " + viewerCode),
                new Label("Title:"), titleField);
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        final String userid = userNameField.getText()+generatedUserId;
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                Map<String, String> result = new HashMap<>();
                result.put("ownerId", userid);
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



    private void joinDocumentOnServer(String title, String code ,String userId) {
        System.out.println("Joining " + title  + " With code: " + code);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL+"documents/by-share-code/" + code+"/"+userId))
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

    private void handleImportDocument() {
        System.out.println("[DEBUG] handleImportDocument() called - opening file chooser");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Text File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File selectedFile = fileChooser.showOpenDialog(root.getScene().getWindow());
        System.out.println("[DEBUG] Selected file: " + (selectedFile != null ? selectedFile.getAbsolutePath() : "null"));

        if (selectedFile != null) {
            System.out.println("[DEBUG] Showing import configuration dialog");
            Dialog<Map<String, String>> dialog = new Dialog<>();
            dialog.setTitle("Create Document from Import");

            TextField docNameField = new TextField();
            docNameField.setPromptText("Document Name");
            TextField userNameField = new TextField();
            userNameField.setPromptText("Your Name");
            String editorCode = generateRandomCode();
            String viewerCode = generateRandomCode();
            System.out.println("[DEBUG] Generated codes - Editor: " + editorCode + " Viewer: " + viewerCode);

            VBox content = new VBox(10,
                    new Label("Document Name:"), docNameField,
                    new Label("Editor Code: " + editorCode),
                    new Label("Viewer Code: " + viewerCode),
                    new Label("Your Name:"), userNameField);
            content.setPadding(new Insets(15));

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(button -> {
                System.out.println("[DEBUG] Dialog button clicked: " + button.getText());
                if (button == ButtonType.OK) {
                    Map<String, String> result = new HashMap<>();
                    result.put("docName", docNameField.getText());
                    result.put("userName", userNameField.getText());
                    result.put("editorCode", editorCode);
                    result.put("viewerCode", viewerCode);
                    System.out.println("[DEBUG] Dialog results: " + result);
                    return result;
                }
                return null;
            });

            dialog.showAndWait().ifPresent(inputs -> {
                System.out.println("[DEBUG] Processing dialog inputs: " + inputs);
                String docName = inputs.get("docName");
                String userName = inputs.get("userName");
                String editorCodeFinal = inputs.get("editorCode");
                String viewerCodeFinal = inputs.get("viewerCode");

                if (docName.isEmpty() || userName.isEmpty()) {
                    System.out.println("[ERROR] Validation failed - empty docName or userName");
                    showErrorDialog("Document name and user name are required");
                    return;
                }

                String userId = userName + generatedUserId;
                System.out.println("[DEBUG] Generated user ID: " + userId);
                importDocument(userId, docName, selectedFile, editorCodeFinal, viewerCodeFinal);
            });
        }
    }

    private void importDocument(String userId, String title, File file, String editorCode, String viewerCode) {
        System.out.println("[DEBUG] importDocument() called");
        System.out.println("[DEBUG] Parameters - userId: " + userId + ", title: " + title +
                ", file: " + file.getName() + ", editorCode: " + editorCode +
                ", viewerCode: " + viewerCode);

        // Show loading indicator
        ProgressIndicator progress = new ProgressIndicator();
        Dialog<Void> loadingDialog = new Dialog<>();
        loadingDialog.getDialogPane().setContent(progress);
        loadingDialog.getDialogPane().getButtonTypes().clear();
        loadingDialog.show();
        loadingDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        System.out.println("[DEBUG] Showing loading dialog");

        try {
            // Build URL with parameters
            String url = API_URL + "documents/import";
            System.out.println("[DEBUG] Target URL: " + url);

            // Create multipart request
            System.out.println("[DEBUG] Creating multipart request");
             HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                    .POST(createMultipartBody(userId, title, editorCode, viewerCode, file))
                    .build();
            System.out.println("[DEBUG] Request created successfully");

            System.out.println("[DEBUG] Sending async request...");
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("[DEBUG] Received response - Status: " + response.statusCode());
                        System.out.println("[DEBUG] Response body: " + response.body());

                        Platform.runLater(() -> {
                            loadingDialog.close();
                            System.out.println("[DEBUG] Closed loading dialog");

                            if (response.statusCode() == 200) {
                                try {
                                    System.out.println("[DEBUG] Parsing successful response");
                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonNode json = mapper.readTree(response.body());
                                    String docId = json.get("id").asText();
                                    System.out.println("[DEBUG] Document created with ID: " + docId);

                                    openDocument(title, docId, "editor");
                                    fetchDocuments(); // Refresh the document list
                                    System.out.println("[DEBUG] Document opened and list refreshed");
                                } catch (Exception e) {
                                    System.out.println("[ERROR] Error parsing response: " + e.getMessage());
                                    e.printStackTrace();
                                    showErrorDialog("Error parsing response: " + e.getMessage());
                                }
                            } else {
                                System.out.println("[ERROR] Import failed with status: " + response.statusCode());
                                showErrorDialog("Import failed: " + response.body());
                            }
                        });
                    })
                    .exceptionally(e -> {
                        System.out.println("[ERROR] Exception during request: " + e.getMessage());
                        e.printStackTrace();
                        Platform.runLater(() -> {

                            showErrorDialog("Import error: " + e.getMessage());
                        });
                        return null;
                    });
        } catch (Exception e) {
            System.out.println("[ERROR] Exception in importDocument: " + e.getMessage());
            e.printStackTrace();
            loadingDialog.close();
            showErrorDialog("Error preparing request: " + e.getMessage());
        }finally {
            loadingDialog.close();
        }
    }

    private static HttpRequest.BodyPublisher ofMimeMultipartData(Map<String, String> data, Path file) throws IOException {
        System.out.println("[DEBUG] Creating multipart form data");
        var boundary = new StringBuilder().append("-------Java11Client").append(UUID.randomUUID()).append("-------");
        System.out.println("[DEBUG] Generated boundary: " + boundary);

        List<byte[]> byteArrays = new ArrayList<>();

        // Add text fields
        System.out.println("[DEBUG] Adding text fields to multipart data");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            System.out.println("[DEBUG] Adding field: " + entry.getKey() + " = " + entry.getValue());
            byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add((entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }

        // Add file part
        System.out.println("[DEBUG] Adding file part to multipart data: " + file.getFileName());
        byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getFileName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Type: text/plain\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(Files.readAllBytes(file));
        byteArrays.add(("\r\n").getBytes(StandardCharsets.UTF_8));

        // Add end boundary
        System.out.println("[DEBUG] Adding end boundary");
        byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    private HttpRequest.BodyPublisher createMultipartBody(String userId, String title,
                                                          String editorCode, String viewerCode, File file) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8), true);

        // Add text parts
        addFormField(writer, "userId", userId);
        addFormField(writer, "title", title);
        addFormField(writer, "editorCode", editorCode);
        addFormField(writer, "viewerCode", viewerCode);

        // Add file part
        writer.append("--").append(BOUNDARY).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");
        writer.append("Content-Type: text/plain\r\n\r\n");
        writer.flush();

        Files.copy(file.toPath(), byteArrayOutputStream);
        writer.append("\r\n");

        // End boundary
        writer.append("--").append(BOUNDARY).append("--\r\n");
        writer.flush();

        return HttpRequest.BodyPublishers.ofByteArray(byteArrayOutputStream.toByteArray());
    }

    private void addFormField(PrintWriter writer, String name, String value) {
        writer.append("--").append(BOUNDARY).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        writer.append(value).append("\r\n");
        writer.flush();
    }

    public BorderPane getRoot() {
        return root;
    }
    private String generateRandomCode() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}