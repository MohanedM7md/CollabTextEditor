package com.example.server.dto.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConnectRequest {
    private String userId;
    private String shareCode; // Either editorCode or viewerCode

    // Optional: Add validation annotations
    // @NotBlank
    // @Size(min = 8, max = 8) // If your share codes are 8 characters
}