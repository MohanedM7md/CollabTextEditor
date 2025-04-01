module com.editor.collabtexteditor {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.editor.collabtexteditor to javafx.fxml;
    exports com.editor.collabtexteditor;
}