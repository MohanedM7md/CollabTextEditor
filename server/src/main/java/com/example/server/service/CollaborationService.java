package com.example.server.service;
import com.example.server.CRDT.operations.Operation;
import com.example.server.model.Document;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CollaborationService {

    private final DocumentService documentService;
    private final SessionRegistry sessionRegistry;

    public CollaborationService(DocumentService documentService, SessionRegistry sessionRegistry) {
        this.documentService = documentService;
        this.sessionRegistry = sessionRegistry;
    }

    public void handleInsert(String docId, InsertRequest request) {
        documentService.findById(docId).ifPresent(document -> {
            CRDTDocument crdtDoc = document.getCrdtDocument();
            try {
                crdtDoc.insert(request.getValue(), request.getPosition(), request.getUserId());
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    public void handleDelete(String docId, DeleteRequest request) {
        documentService.findById(docId).ifPresent(document -> {
            CRDTDocument crdtDoc = document.getCrdtDocument();
            try {
                crdtDoc.delete(request.getPosition(), request.getUserId());
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    public void handleCursorUpdate(String docId, CursorUpdateRequest request) {
        documentService.findById(docId).ifPresent(document -> {
            document.getCrdtDocument().updateCursor(
                    request.getUserId(),
                    request.getPosition(),
                    request.getColor()
            );
        });
    }

    public void handleUndo(String docId, String userId) {
        documentService.findById(docId).ifPresent(document -> {
            document.getCrdtDocument().undo(userId);
        });
    }

    public void handleRedo(String docId, String userId) {
        documentService.findById(docId).ifPresent(document -> {
            document.getCrdtDocument().redo(userId);
        });
    }
}
