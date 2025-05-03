package com.example.server.dto.requests;

import com.example.server.model.CursorPosition;

import java.util.Collection;

public record DocumentStateResponse(
        String text,
        Collection<CursorPosition> cursors,
        String activeUsers
) {}