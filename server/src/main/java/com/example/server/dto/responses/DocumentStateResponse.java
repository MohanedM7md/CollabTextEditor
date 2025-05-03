package com.example.server.dto.responses;

import com.example.server.model.CursorPosition;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter @Setter
public class DocumentStateResponse {
    private String text;
    private Collection<CursorPosition> cursors;
    private Collection<String> activeUsers;
}