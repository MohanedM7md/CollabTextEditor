package com.example.server.dto.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CursorUpdateRequest {
    private String userId;
    private int position; // Cursor position in the document
    private String color; // Hex color code like "#FF0000"

    // Optional validation example:
    // @Min(0) for position
    // @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$") for color
}