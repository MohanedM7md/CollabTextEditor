package com.example.server.model;

import com.example.server.CRDT.CRDTDocument;
import lombok.Getter;
import lombok.Setter;

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

    public Document(String ownerId) {
        this();
        this.ownerId = ownerId;
        this.crdtDocument = new CRDTDocument(id, ownerId);
        this.editorCode = UUID.randomUUID().toString();
        this.viewerCode = UUID.randomUUID().toString();
    }

    public enum DocumentStatus {
        ACTIVE, ARCHIVED, DELETED
    }

    public boolean canEdit(String userId) {
       return this.crdtDocument.canEdit(userId);
    }

    public boolean canView(String userId) {
        return this.crdtDocument.canView(userId)|| canEdit(userId);
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}