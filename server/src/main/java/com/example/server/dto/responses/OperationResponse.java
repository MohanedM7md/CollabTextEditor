package com.example.server.dto.responses;

import com.example.server.CRDT.operations.Operation;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OperationResponse {
    private String documentId;
    private Operation operation;
    private String userId;
    private long timestamp;
    private boolean success;
    private String message; // Optional: for error messages

    // Static factory methods for success/failure cases
    public static OperationResponse success(String documentId, Operation operation, String userId) {
        OperationResponse response = new OperationResponse();
        response.setDocumentId(documentId);
        response.setOperation(operation);
        response.setUserId(userId);
        response.setTimestamp(System.currentTimeMillis());
        response.setSuccess(true);
        return response;
    }

    public static OperationResponse failure(String documentId, String userId, String errorMessage) {
        OperationResponse response = new OperationResponse();
        response.setDocumentId(documentId);
        response.setUserId(userId);
        response.setTimestamp(System.currentTimeMillis());
        response.setSuccess(false);
        response.setMessage(errorMessage);
        return response;
    }
}