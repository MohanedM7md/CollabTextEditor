package com.example.server.CRDT;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class CharItem {
    private final String id;         // userId + "-" + timestamp
    private final char value;        // The character value
    private boolean isDeleted;       // Delete flag for undo/redo
    private String color;            // Highlight color
    private String comment;          // Associated comment
    private final long timestamp;    // Creation timestamp
    private final String userId;     // Creator user ID
    private List<Integer> path;      // Tree path for positioning

    public CharItem(char value, String userId) {

        this.value = value;
        this.userId = userId;
        this.timestamp = System.nanoTime();
        this.id = userId + "-" + timestamp;
        this.isDeleted = false;
        this.color = null;
        this.comment = null;
        this.path = new ArrayList<>(); // Will be set during insertion
    }

    // For creating items with specific paths (used in merging)
    public CharItem(char value, String userId, long timestamp, List<Integer> path) {
        this.value = value;
        this.userId = userId;
        this.timestamp = timestamp;
        this.id = userId + "-" + timestamp;
        this.path = new ArrayList<>(path);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharItem charItem = (CharItem) o;
        return id.equals(charItem.id);
    }

    @Override
    public String toString() {
        return "CharItem{" + value + "}@" + path + "[" + userId + "]";
    }
}