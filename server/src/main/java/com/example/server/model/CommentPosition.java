package com.example.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CommentPosition {
    private final String id;
    private final String userId;
    private final String color;
    private int StartPos;
    private int EndPos;
    private String text;


    public CommentPosition(String id, String userId, String color, int startPos, int endPos, String text) {
        this.id = id;
        this.userId = userId;
        this.color = color;
        StartPos = startPos;
        EndPos = endPos;
        this.text = text;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CommentPosition other)) return false;
        return userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}