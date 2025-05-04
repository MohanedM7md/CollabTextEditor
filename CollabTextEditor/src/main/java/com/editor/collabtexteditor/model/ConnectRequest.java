package com.editor.collabtexteditor.model;

public class ConnectRequest {
    private String userId;
    private String shareCode;

    public ConnectRequest() {}
    public ConnectRequest(String userId, String shareCode) {
        this.userId = userId;
        this.shareCode = shareCode;
    }
    public String getUserId() { return userId; }
    public String getShareCode() { return shareCode; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; }
}