package com.editor.collabtexteditor.model;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
@Getter @Setter
public class ConnectRequest {
    private String userId;
    private boolean isEditor;
    public ConnectRequest() {
    }
    public ConnectRequest(@JsonProperty("userId") String userId, @JsonProperty("isEditor") boolean isEditor) {
        this.userId = userId;
        this.isEditor = isEditor;
    }
}