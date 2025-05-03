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

    public Operation applyOperation(String docId, String userId, Operation operation) {
        Optional<Document> optionalDoc = documentService.findById(docId);
        if (optionalDoc.isEmpty()) {
            System.out.println("Document not found: " + docId);
            return operation;
        }

        Document doc = optionalDoc.get();
        if (!doc.canEdit(userId)) {
            System.out.println("User " + userId + " not authorized to edit document " + docId);
            return operation;
        }

        doc.getCrdtDocument().applyOperation(operation);
        doc.updateTimestamp();
        documentService.save(doc);
        System.out.println("Operation applied to document " + docId);
        return operation;
    }

    public boolean updateCursor(String docId, String userId, int position, String color) {
        Optional<Document> optionalDoc = documentService.findById(docId);
        if (optionalDoc.isEmpty()) {
            System.out.println("Document not found: " + docId);
            return false;
        }

        Document doc = optionalDoc.get();
        doc.getCrdtDocument().updateCursor(userId, position, color);
        System.out.println("Cursor updated for user " + userId + " in document " + docId);
        return true;
    }
}
