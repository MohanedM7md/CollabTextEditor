package com.editor.collabtexteditor.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

}