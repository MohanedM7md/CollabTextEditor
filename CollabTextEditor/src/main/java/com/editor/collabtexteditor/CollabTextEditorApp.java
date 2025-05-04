package com.editor.collabtexteditor;

import com.editor.collabtexteditor.controllers.DocumentsOverviewController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class CollabTextEditorApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        DocumentsOverviewController overviewController = new DocumentsOverviewController();
        Scene scene = new Scene(overviewController.getRoot(), 1000, 700);

        // Load CSS
        try {
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("main.css")).toExternalForm()
            );
        } catch (Exception e) {
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