package com.editor.collabtexteditor.model;

import java.util.Collection;

public class DocumentStateResponse {
    private String text;
    private Collection<Object> cursors; // You can make CursorPosition class for detailed handling
    private String operationType;
    private String triggeringUser;

    public DocumentStateResponse() {}

    // Getters/Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Collection<Object> getCursors() { return cursors; }
    public void setCursors(Collection<Object> cursors) { this.cursors = cursors; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getTriggeringUser() { return triggeringUser; }
    public void setTriggeringUser(String triggeringUser) { this.triggeringUser = triggeringUser; }
}