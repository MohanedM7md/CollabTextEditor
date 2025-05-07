package com.example.server.model;

import lombok.Getter;

@Getter
public class CursorPosition {
    private final String userId;
    private int position;
    private final String color;

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