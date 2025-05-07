package com.editor.collabtexteditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class CursorOverlayTest extends Application {
    @Override
    public void start(Stage primaryStage) {
        TextArea textArea = new TextArea();
        textArea.setText("This is a test\nfor remote cursor visualization.");

        Pane overlayPane = new Pane();
        overlayPane.setMouseTransparent(true); // Allow clicks to pass through
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.prefWidthProperty().bind(textArea.widthProperty());
        overlayPane.prefHeightProperty().bind(textArea.heightProperty());

        // Create a red rectangle to simulate a cursor
        Rectangle test = new Rectangle(2, 18, Color.RED);
        test.setLayoutX(50);
        test.setLayoutY(25);
        overlayPane.getChildren().add(test);

        StackPane root = new StackPane();
        root.getChildren().addAll(textArea, overlayPane); // Overlay goes above textArea

        Scene scene = new Scene(root, 500, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Cursor Overlay Test");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
