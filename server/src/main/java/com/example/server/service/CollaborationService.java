package com.example.server.service;
import com.example.server.CRDT.CRDTDocument;
import com.example.server.dto.requests.*;
import com.example.server.dto.responses.CursorResponse;
import com.example.server.dto.responses.DocumentStateResponse;
import org.springframework.stereotype.Service;

@Service
public class CollaborationService {

    private final DocumentService documentService;

    public CollaborationService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public DocumentStateResponse handleInsert(String docId, InsertRequest request) {
        return documentService.findById(docId).map(document -> {
            CRDTDocument crdt = document.getCrdtDocument();
            crdt.insert(request.getValue(), request.getPosition(), request.getUserId());
            return crdt.getCurrentState("INSERT", request.getUserId());
        }).orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public DocumentStateResponse handleInsertBulk(String docId, BulkInsertRequest request) {
        return documentService.findById(docId).map(document -> {
            CRDTDocument crdt = document.getCrdtDocument();
                crdt.setHistoryEnabled(false);
                for (int i = 0; i < request.getText().length(); i++) {
                    char c = request.getText().charAt(i);
                    crdt.insertBulk(c, i, request.getUserId());
                }
            return crdt.getCurrentState("INSERT-BULK", request.getUserId());
        }).orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public DocumentStateResponse handleDelete(String docId, DeleteRequest request) {
        return documentService.findById(docId).map(document -> {
            CRDTDocument crdt = document.getCrdtDocument();
            crdt.delete(request.getPosition(), request.getUserId());
            return crdt.getCurrentState("DELETE", request.getUserId());
        }).orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public CursorResponse handleCursorUpdate(String docId, CursorUpdateRequest request) {
        return documentService.findById(docId).map(document -> {

            document.getCrdtDocument().updateCursor(
                    request.getUserId(),
                    request.getPosition(),
                    request.getColor()
            );
            return new CursorResponse(
                    request.getUserId(),
                    request.getPosition(),
                    request.getColor(),
                    docId
            );
        }).orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public DocumentStateResponse handleUndo(String docId, UndoRequest request) {
        return documentService.findById(docId).map(document -> {
            CRDTDocument crdt = document.getCrdtDocument();
            crdt.undo(request.getUserId());
            return crdt.getCurrentState("UNDO", request.getUserId());
        }).orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public DocumentStateResponse handleRedo(String docId, RedoRequest request) {
        return documentService.findById(docId).map(document -> {
            CRDTDocument crdt = document.getCrdtDocument();
            crdt.redo(request.getUserId());
            return crdt.getCurrentState("REDO", request.getUserId());
        }).orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public void handleDisconnect(String docId, ConnectRequest request) {
        documentService.findById(docId).ifPresent(document -> {
            // Remove user from CRDT document
            CRDTDocument crdt = document.getCrdtDocument();
            crdt.removeUser(request.getUserId());

            // Save the updated document
            document.setCrdtDocument(crdt);
            documentService.save(document);

            // Log the disconnection
            System.out.printf("User %s disconnected from document %s%n",
                    request.getUserId(), docId);
        });
    }
    public void handleConnect(String docId, ConnectRequest request) {
        documentService.findById(docId).ifPresent(document -> {
            CRDTDocument crdt = document.getCrdtDocument();
            if(request.isEditor())
                crdt.addEditor(request.getUserId());
            else
                crdt.addViewer(request.getUserId());
        });
    }

    public DocumentStateResponse handleAddComment(String docId,
                                                  AddCommentRequest req) {
        return documentService.findById(docId)
                .map(doc -> {
                    CRDTDocument crdt = doc.getCrdtDocument();
                    crdt.addComment(req.getStartPos(),
                            req.getEndPos(),
                            req.getColor(),
                            req.getUserId(),
                            req.getText());
                    return crdt.getCurrentState("ADDCOMMENT", req.getUserId());
                })
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }
}
