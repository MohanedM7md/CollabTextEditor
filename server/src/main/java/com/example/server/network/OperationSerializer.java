package com.example.server.network;

import com.example.server.CRDT.operations.Operation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Component
public class OperationSerializer {
    private final ObjectMapper objectMapper;

    public OperationSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void registerSubtypes() {
        // Register all operation types for polymorphic deserialization
        objectMapper.registerSubtypes(
                new NamedType(com.example.server.CRDT.operations.InsertOperation.class, "INSERT"),
                new NamedType(com.example.server.CRDT.operations.DeleteOperation.class, "DELETE"),
                new NamedType(com.example.server.CRDT.operations.AddCommentOperation.class, "ADDCOMMENT"),
                new NamedType(com.example.server.CRDT.operations.RemoveCommentOperation.class, "REMOVECOMMENT"),
                new NamedType(com.example.server.CRDT.operations.HighlightOperation.class, "HIGHLIGHT")
        );
    }

    public String serialize(Operation operation) throws JsonProcessingException {
        return objectMapper.writeValueAsString(operation);
    }

    public Operation deserialize(String json) throws IOException {
        return objectMapper.readValue(json, Operation.class);
    }

    public <T extends Operation> T deserialize(String json, Class<T> type) throws IOException {
        return objectMapper.readValue(json, type);
    }
}
