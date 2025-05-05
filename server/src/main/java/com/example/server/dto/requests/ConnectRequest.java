package com.example.server.dto.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConnectRequest {
    private String userId;
    private boolean isEditor;
    public ConnectRequest(String userId, boolean isEditor) {
        this.userId = userId;
        this.isEditor = isEditor;
    }
}