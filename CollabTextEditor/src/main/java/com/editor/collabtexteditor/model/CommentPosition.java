package com.editor.collabtexteditor.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CommentPosition {
    private  String id;
    private  String userId;
    private  String color;
    private  String text = "";
    private int StartPos;
    private int EndPos;

    public CommentPosition() {}

    public CommentPosition(String id, String userId, String color, int startPos, int endPos) {
        this.id = id;
        this.userId = userId;
        this.color = color;
        StartPos = startPos;
        EndPos = endPos;
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