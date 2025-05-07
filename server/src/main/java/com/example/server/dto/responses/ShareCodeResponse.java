package com.example.server.dto.responses;
import lombok.Getter;
import lombok.Setter;
@Setter @Getter
public class ShareCodeResponse {

    private String id;
    private String role; // "editor" or "viewer"

    public ShareCodeResponse(String id, String role) {
        this.id = id;
        this.role = role;
    }
}
