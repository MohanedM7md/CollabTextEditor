package com.example.server.service;
import com.example.server.CRDT.operations.Operation;
import com.example.server.model.Document;
import com.example.server.model.UserSession;
import org.springframework.stereotype.Service;

@Service
public class CollaborationService {
    private final DocumentService documentService;
    private final SessionRegistry sessionRegistry;

    public CollaborationService(DocumentService documentService, SessionRegistry sessionRegistry) {
        this.documentService = documentService;
        this.sessionRegistry = sessionRegistry;
    }

    public void applyOperation(String docId, String userId, Operation operation) {
        documentService.findById(docId).ifPresent(doc -> {
            if (doc.canEdit(userId)) {
                doc.getCrdtDocument().applyOperation(operation);
                doc.updateTimestamp();
                documentService.save(doc);
            }
        });
    }

    public void updateCursor(String sessionId, String docId, String userId, int position, String color) {
        sessionRegistry.getSession(sessionId).ifPresent(session -> {
            documentService.findById(docId).ifPresent(doc -> {
                doc.getCrdtDocument().updateCursor(userId, position, color);
                session.updateActivity();
            });
        });
    }
}