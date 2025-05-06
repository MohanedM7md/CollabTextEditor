package com.editor.collabtexteditor.dto.request;

public class RedoRequest {
    private String userId;

    public RedoRequest() {}
    public RedoRequest(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}