package com.example.server.dto.requests;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BulkInsertRequest {
    private String userId;
    private String text;
    private int position;

    BulkInsertRequest(){}
    public BulkInsertRequest(String userId, String text, int position) {
        this.userId = userId;
        this.text = text;
        this.position = position;
    }
}