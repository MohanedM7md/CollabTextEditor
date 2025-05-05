package com.editor.collabtexteditor.model;

public class CursorResponse {
    private String userId;
    private int position;
    private String color;
    private String docId;

    public CursorResponse() {}

    public CursorResponse(String userId, int position, String color) {
        this.userId = userId;
        this.position = position;
        this.color = color;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public int getPosition() {
        return position;
    }
    public void setPosition(int position) {
        this.position = position;
    }
    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        this.color = color;
    }
    public String getDocId() {
        return docId;
    }
    public void setDocId(String docId) {
        this.docId = docId;
    }
}