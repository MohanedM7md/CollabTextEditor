package com.example.server.service;
import com.example.server.CRDT.CRDTDocument;
import com.example.server.dto.requests.*;
import com.example.server.dto.responses.CursorResponse;
import com.example.server.dto.responses.DocumentStateResponse;
import org.springframework.stereotype.Service;

@Service
public class CollaborationService {

    private final DocumentService documentService;

    public CollaborationService(DocumentService documentService, SessionRegistry sessionRegistry) {
        this.documentService = documentService;
    }

    public DocumentStateResponse handleInsert(String docId, InsertRequest request) {
        return documentService.findById(docId).map(document -> {
            CRDTDocument crdt = document.getCrdtDocument();
            crdt.insert(request.getValue(), request.getPosition(), request.getUserId());
            return crdt.getCurrentState("INSERT", request.getUserId());
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

    public DocumentStateResponse handleDisconnect(String docId, ConnectRequest request) {
        return documentService.findById(docId).map(document -> {
            CRDTDocument crdt = document.getCrdtDocument();
            crdt.removeUser(request.getUserId());  // You'll need to implement this in CRDTDocument
            return crdt.getCurrentState("Remove", request.getUserId());
        }).orElseThrow(() -> new RuntimeException("Document not found"));
    }

}
