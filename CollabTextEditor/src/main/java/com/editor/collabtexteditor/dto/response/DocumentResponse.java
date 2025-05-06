package com.editor.collabtexteditor.dto.response;

import com.editor.collabtexteditor.model.CommentPosition;
import com.editor.collabtexteditor.model.CursorPosition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
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

    @Getter
    public static class CrdtDocument {
        private String activeUsers;
        private String text;
        private List<CursorPosition> allCursors;
        private List<CommentPosition> allComments;

        public CrdtDocument(
                String activeUsers,
                String text,
                List<CursorPosition> allCursors,
                List<CommentPosition> allComments) {
            this.activeUsers = activeUsers;
            this.text = text;
            this.allCursors = allCursors;
            this.allComments = allComments;
        }


    }
}
