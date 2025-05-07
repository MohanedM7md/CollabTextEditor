package com.editor.collabtexteditor.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InsertRequest {
    // Getters/Setters
    private String userId;
    private char value;
    private int position;

    public InsertRequest() {}
    public InsertRequest(String userId, char value, int position) {
        this.userId = userId;
        this.value = value;
        this.position = position;
    }

}