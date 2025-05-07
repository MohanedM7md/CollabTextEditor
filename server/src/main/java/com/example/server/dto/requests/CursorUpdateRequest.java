// CursorUpdateRequest.java
package com.example.server.dto.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CursorUpdateRequest {
    private String userId;
    private int position;
    private String color;
}
