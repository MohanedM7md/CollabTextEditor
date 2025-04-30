package com.example.server.model;

public class User {
    private String userId;
    private String name;
    private String role;       // "editor" or "viewer"
    private int currentLine;   // NEW: line number the user is on (e.g., 2)

    public User() {}

    public User(String userId, String name, String role, int currentLine) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.currentLine = currentLine;
    }

    // Getters and Setters

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getCurrentLine() { return currentLine; }
    public void setCurrentLine(int currentLine) { this.currentLine = currentLine; }
}
