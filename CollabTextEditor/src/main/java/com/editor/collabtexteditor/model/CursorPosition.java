package com.editor.collabtexteditor.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursorPosition {
    private String userId;
    private int position;
    private String color;

    // No-arg constructor required by Jackson
    public CursorPosition() {}

    public CursorPosition(String userId, int position, String color) {
        this.userId = userId;
        this.position = position;
        this.color = color;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CursorPosition)) return false;
        CursorPosition other = (CursorPosition) obj;
        return userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
