package com.editor.collabtexteditor.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

}