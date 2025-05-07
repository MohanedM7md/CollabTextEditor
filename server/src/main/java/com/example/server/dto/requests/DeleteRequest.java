// DeleteRequest.java
package com.example.server.dto.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DeleteRequest {
    private String userId;
    private int position;
}