package com.example.server.CRDT;

import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class CharNode {
    @Setter
    private String id;             // Unique ID (e.g., userId:timestamp)
    @Setter
    private String value;          // ASCII character
    @Setter
    private boolean deleted;       // Tombstone flag
    private List<CharNode> children;

    public CharNode(String id, String value) {
        this.id = id;
        this.value = value;
        this.deleted = false;
        this.children = new ArrayList<>();
    }

    // Getters and Setters

    public String getId() { return id; }

    public String getValue() { return value; }

    public boolean isDeleted() { return deleted; }

    public List<CharNode> getChildren() { return children; }

    public void addChild(CharNode child) {
        this.children.add(child);
        sortChildren();
    }

    private void sortChildren() {
        children.sort((a, b) -> {
            // Order by timestamp descending, then user ID ascending
            String[] aParts = a.id.split(":");
            String[] bParts = b.id.split(":");

            int timeCompare = bParts[1].compareTo(aParts[1]); // newer first
            if (timeCompare != 0) return timeCompare;

            return aParts[0].compareTo(bParts[0]); // lower userId wins
        });
    }
}
