package com.example.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserSession {
    private String sessionId;
    private String userId;
    private String documentId;
    private long lastActive;

    public UserSession(String sessionId, String userId, String documentId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.documentId = documentId;
        this.lastActive = System.currentTimeMillis();
    }

    public void updateActivity() {
        this.lastActive = System.currentTimeMillis();
    }
}