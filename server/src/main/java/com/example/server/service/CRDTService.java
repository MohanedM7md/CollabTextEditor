/*
package com.example.server.service;

import com.example.server.model.Operation;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CRDTService {

    // One CRDTManager per session
    private final Map<String, CRDTManager> sessionCrdts = new ConcurrentHashMap<>();

    public Operation applyOperation(String sessionId, Operation operation) {
        CRDTManager crdt = sessionCrdts.computeIfAbsent(sessionId, id -> new CRDTManager());
        crdt.apply(operation);
        return operation; // Optionally return transformed version (for OT systems)
    }

    public String getDocumentText(String sessionId) {
        CRDTManager crdt = sessionCrdts.get(sessionId);
        return crdt != null ? crdt.getPlainText() : "";
    }

    public void restoreOperations(String sessionId, Iterable<Operation> ops) {
        CRDTManager crdt = sessionCrdts.computeIfAbsent(sessionId, id -> new CRDTManager());
        for (Operation op : ops) {
            crdt.apply(op);
        }
    }
}
*/
