package com.editor.collabtexteditor.dto.request;

public class InsertRequest {
    private String userId;
    private char value;
    private int position;

    public InsertRequest() {}
    public InsertRequest(String userId, char value, int position) {
        this.userId = userId;
        this.value = value;
        this.position = position;
    }

    // Getters/Setters
    public String getUserId() { return userId; }
    public char getValue() { return value; }
    public int getPosition() { return position; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setValue(char value) { this.value = value; }
    public void setPosition(int position) { this.position = position; }
}