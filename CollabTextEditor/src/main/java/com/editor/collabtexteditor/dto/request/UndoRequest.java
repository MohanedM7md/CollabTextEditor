package com.editor.collabtexteditor.dto.request;

public class UndoRequest {
    private String userId;

    public UndoRequest() {}
    public UndoRequest(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}