package com.example.server.dto.requests;

import com.example.server.CRDT.operations.Operation;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OperationRequest {
    private String userId;
    private Operation operation;
    private long timestamp; // Optional: For conflict resolution

    // You might want to add:
    // @NotNull for operation
}