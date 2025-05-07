package com.editor.collabtexteditor.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddCommentRequest {

    private String userId;
    private int    startPos;

    private int    endPos;          // exclusive
    private String color;
    private String text;

    public AddCommentRequest() {}   // ← Jackson / Gson need no-arg ctor

    public AddCommentRequest(String userId, int startPos,
                             int endPos, String color, String text) {

        this.userId   = userId;
        this.startPos = startPos;
        this.endPos   = endPos;
        this.color    = color;
        this.text     = text;
    }
}