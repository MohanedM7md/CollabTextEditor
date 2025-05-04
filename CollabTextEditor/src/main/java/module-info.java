module com.editor.collabtexteditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.fasterxml.jackson.databind;
    exports com.editor.collabtexteditor.model;

    requires java.net.http;
    requires static lombok;
    requires spring.messaging;
    requires spring.websocket;
    requires spring.core;

    opens com.editor.collabtexteditor to javafx.fxml;
    exports com.editor.collabtexteditor;
    exports com.editor.collabtexteditor.controllers;
    opens com.editor.collabtexteditor.controllers to javafx.fxml;
}