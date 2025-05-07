package com.example.server.dto.responses;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CursorResponse {
    private String userId;
    private int position;
    private String color;
    private String documentId;
    public CursorResponse(String userId, int position, String color, String documentId) {
        this.userId = userId;
        this.position = position;
        this.color = color;
        this.documentId = documentId;
    }
}