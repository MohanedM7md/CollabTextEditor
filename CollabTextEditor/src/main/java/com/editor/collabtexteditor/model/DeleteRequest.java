package com.editor.collabtexteditor.model;

public class DeleteRequest {
    private String userId;
    private int position;

    public DeleteRequest() {}
    public DeleteRequest(String userId, int position) {
        this.userId = userId;
        this.position = position;
    }

    public String getUserId() { return userId; }
    public int getPosition() { return position; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setPosition(int position) { this.position = position; }
}