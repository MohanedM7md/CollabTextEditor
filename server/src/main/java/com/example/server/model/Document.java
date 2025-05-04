package com.example.server.model;

import com.example.server.CRDT.CRDTDocument;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter @Setter
public class Document {
    private String id;
    private String title;
    private String ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String editorCode;
    private String viewerCode;
    private CRDTDocument crdtDocument;
    private DocumentStatus status = DocumentStatus.ACTIVE;

    public Document() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Document(String ownerId, String editorCode ,String viewerCode,String title) {
        this();
        this.ownerId = ownerId;
        this.crdtDocument = new CRDTDocument(id, ownerId);
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.title=title;
    }

    public enum DocumentStatus {
        ACTIVE, ARCHIVED, DELETED
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    public void exportToTextFile(File file) throws IOException {
        if (crdtDocument == null) {
            throw new IllegalStateException("No CRDT document to export.");
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(crdtDocument.getText());
        }
    }

    public void importFromTextFile(File file, String userId) throws IOException {
        if (crdtDocument == null) {
            this.crdtDocument = new CRDTDocument(this.id, userId);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (char c : line.toCharArray()) {
                    crdtDocument.insert(c, crdtDocument.getText().length(), userId);
                }
                crdtDocument.insert('\n', crdtDocument.getText().length(), userId);
            }
        }
    }
}