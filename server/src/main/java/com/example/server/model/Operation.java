package com.example.server.model;

public class Operation {
    private String op;          // "insert" or "delete"
    private String value;       // Character inserted (or null if delete)
    private String parentId;    // ID of the parent character
    private String id;          // Unique ID for this character (e.g., userId:timestamp)
    private String userId;      // Who performed the operation
    private String timestamp;   // Logical or system time

    public Operation() {}

    public Operation(String op, String value, String parentId, String id, String userId, String timestamp) {
        this.op = op;
        this.value = value;
        this.parentId = parentId;
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    // Getters and Setters

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
