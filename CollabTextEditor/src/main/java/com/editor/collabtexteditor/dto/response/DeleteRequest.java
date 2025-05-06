package com.editor.collabtexteditor.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DeleteRequest {
    private String userId;
    private int position;

    public DeleteRequest() {}
    public DeleteRequest(String userId, int position) {
        this.userId = userId;
        this.position = position;
    }

}