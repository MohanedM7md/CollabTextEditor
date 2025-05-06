package com.example.server.dto.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddCommentRequest {

    private String userId;
    private int    startPos;
    private int    endPos;          // exclusive
    private String text;
    private String color;           // e.g. "#ffcc00"

    public AddCommentRequest() {}   // ← Jackson / Gson need no-arg ctor

    public AddCommentRequest(String userId, int startPos,
                             int endPos, String color) {
        this.userId   = userId;
        this.startPos = startPos;
        this.endPos   = endPos;
        this.color    = color;
    }


}