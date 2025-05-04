package com.editor.collabtexteditor.controllers;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class CreateDocumentDialog extends Dialog<String> {
    public CreateDocumentDialog(String userId) {
        setTitle("Document Created");
        setHeaderText("Share these codes with collaborators");

        // Editor code (full access)
        String editorCode = generateShareCode();
        TextField editorField = new TextField(editorCode);
        editorField.setEditable(false);

        // Viewer code (read-only)
        String viewerCode = generateShareCode();
        TextField viewerField = new TextField(viewerCode);
        viewerField.setEditable(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Editor Code:"), editorField);
        grid.addRow(1, new Label("Viewer Code:"), viewerField);

        getDialogPane().setContent(new VBox(10,
                new Label("Document created successfully!"),
                grid,
                new Label("Your User ID: " + userId)
        ));

        getDialogPane().getButtonTypes().add(ButtonType.OK);

        // Return the editor code when dialog closes
        setResultConverter(buttonType -> editorCode);
    }

    private String generateShareCode() {
        return Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 8);
    }
}