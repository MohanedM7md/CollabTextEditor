package com.editor.collabtexteditor.controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.UUID;

public class DocumentsOverviewController {
    private final BorderPane root = new BorderPane();
    private final String generatedUserId;

    public DocumentsOverviewController() {
        this.generatedUserId = UUID.randomUUID().toString().substring(0, 8);
        initializeUI();
    }

    private void initializeUI() {
        // Header
        Label titleLabel = new Label("My Documents");
        titleLabel.setFont(new Font(24));
        HBox header = new HBox(titleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));

        // Document Grid
        TilePane documentsGrid = new TilePane();
        documentsGrid.setPadding(new Insets(20));
        documentsGrid.setHgap(20);
        documentsGrid.setVgap(20);
        documentsGrid.setPrefColumns(3);

        // Sample documents (in real app, fetch from server)
        for (int i = 1; i <= 5; i++) {
            VBox docBox = createDocumentBox("Document " + i);
            documentsGrid.getChildren().add(docBox);
        }

        ScrollPane scrollPane = new ScrollPane(documentsGrid);
        scrollPane.setFitToWidth(true);

        // Create New Button
        Button createNewBtn = new Button("Create New Document");
        createNewBtn.setOnAction(e -> showCreateDocumentDialog());

        VBox centerBox = new VBox(20, scrollPane, createNewBtn);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(20));

        root.setTop(header);
        root.setCenter(centerBox);
    }

    private VBox createDocumentBox(String docName) {
        Rectangle docThumbnail = new Rectangle(150, 200);
        docThumbnail.setFill(Color.LIGHTGRAY);
        docThumbnail.setStroke(Color.DARKGRAY);

        Label nameLabel = new Label(docName);
        nameLabel.setMaxWidth(150);
        nameLabel.setAlignment(Pos.CENTER);

        Button openBtn = new Button("Open");
        openBtn.setMaxWidth(Double.MAX_VALUE);
        openBtn.setOnAction(e -> openDocument(docName));

        VBox docBox = new VBox(10, docThumbnail, nameLabel, openBtn);
        docBox.setAlignment(Pos.CENTER);
        docBox.setPadding(new Insets(10));
        docBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");

        return docBox;
    }

    private void showCreateDocumentDialog() {
        // Create document and show join codes
        CreateDocumentDialog dialog = new CreateDocumentDialog(generatedUserId);
        // Open the document in edit mode
        dialog.showAndWait().ifPresent(this::openDocument);
    }

    private void openDocument(String docId) {
        CollabSessionController sessionController = new CollabSessionController(docId, generatedUserId, "editor");
        root.getScene().setRoot(sessionController.getRoot());
    }

    public BorderPane getRoot() {
        return root;
    }
}