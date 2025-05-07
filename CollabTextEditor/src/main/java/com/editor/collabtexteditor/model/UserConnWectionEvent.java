package com.editor.collabtexteditor.model;

import com.editor.collabtexteditor.dto.request.ConnectRequest;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
@Setter
@Getter
public class UserConnWectionEvent {
    // Getters and setters
    private ConnectRequest request;
    private String eventType;

    public UserConnWectionEvent( @JsonProperty("request") ConnectRequest request, @JsonProperty("eventType") String eventType) {
        this.request = request;
        this.eventType = eventType;
    }


}