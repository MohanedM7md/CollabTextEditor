package com.editor.collabtexteditor;

import com.editor.collabtexteditor.controllers.CollabSessionController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class CollabTextEditorApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        CollabSessionController controller = new CollabSessionController();
        Scene scene = new Scene(controller.getRoot(), 800, 600);

        // Try to load CSS, but don't fail if not found
        try {
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("styles.main.css")).toExternalForm()
            );
        } catch (NullPointerException e) {
            System.err.println("Warning: CSS file not found. Using default styling.");
        }

        primaryStage.setTitle("Collaborative Text Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}