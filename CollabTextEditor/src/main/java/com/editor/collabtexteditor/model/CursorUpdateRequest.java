package com.editor.collabtexteditor.model;

public class CursorUpdateRequest {
    private String userId;
    private int position;
    private String color;

    public CursorUpdateRequest() {}
    public CursorUpdateRequest(String userId, int position, String color) {
        this.userId = userId;
        this.position = position;
        this.color = color;
    }

    public String getUserId() { return userId; }
    public int getPosition() { return position; }
    public String getColor() { return color; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setPosition(int position) { this.position = position; }
    public void setColor(String color) { this.color = color; }
}