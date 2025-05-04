package com.editor.collabtexteditor.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class DocumentResponse {
    private String id;
    private String title;
    private String ownerId;
    private String createdAt;
    private String updatedAt;
    private String editorCode;
    private String viewerCode;
    private String status;
    private CrdtDocument crdtDocument;
    public DocumentResponse() {}
    @JsonCreator
    public DocumentResponse(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("ownerId") String ownerId,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("updatedAt") String updatedAt,
            @JsonProperty("editorCode") String editorCode,
            @JsonProperty("viewerCode") String viewerCode,
            @JsonProperty("status") String status,
            @JsonProperty("crdtDocument") CrdtDocument crdtDocument) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.status = status;
        this.crdtDocument = crdtDocument;
    }
    // Getters and setters

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getOwnerId() { return ownerId; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getEditorCode() { return editorCode; }
    public String getViewerCode() { return viewerCode; }
    public String getStatus() { return status; }
    public CrdtDocument getCrdtDocument() { return crdtDocument; }

    public static class CrdtDocument {
        private String activeUsers;
        private String text;
        private List<CursorPosition> allCursors;

        public CrdtDocument(
               String activeUsers,
               String text,
               List<CursorPosition> allCursors) {
            this.activeUsers = activeUsers;
            this.text = text;
            this.allCursors = allCursors;
        }


        public String getActiveUsers() { return activeUsers; }
        public String getText() { return text; }
        public List<CursorPosition>getAllCursors() { return allCursors; }
    }
}
