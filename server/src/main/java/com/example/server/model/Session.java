package com.example.server.model;

import java.util.*;

public class Session {
    private String sessionId;
    private String editorCode;
    private String viewerCode;
    private List<User> activeUsers;
    private List<Operation> documentOps; // CRDT history

    public Session() {
        this.activeUsers = new ArrayList<>();
        this.documentOps = new ArrayList<>();
    }

    public Session(String sessionId, String editorCode, String viewerCode) {
        this.sessionId = sessionId;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.activeUsers = new ArrayList<>();
        this.documentOps = new ArrayList<>();
    }

    // Getters and Setters

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getEditorCode() { return editorCode; }
    public void setEditorCode(String editorCode) { this.editorCode = editorCode; }

    public String getViewerCode() { return viewerCode; }
    public void setViewerCode(String viewerCode) { this.viewerCode = viewerCode; }

    public List<User> getActiveUsers() { return activeUsers; }
    public void setActiveUsers(List<User> activeUsers) { this.activeUsers = activeUsers; }

    public List<Operation> getDocumentOps() { return documentOps; }
    public void setDocumentOps(List<Operation> documentOps) { this.documentOps = documentOps; }
}

