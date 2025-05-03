package com.example.server.dto.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InsertRequest {
    private String userId;
    private char value;
    private int position;
}
