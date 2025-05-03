package com.example.server.controller;

import com.example.server.CRDT.operations.Operation;
import com.example.server.dto.requests.ConnectRequest;
import com.example.server.dto.requests.CursorUpdateRequest;
import com.example.server.dto.requests.OperationRequest;
import com.example.server.service.CollaborationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CollaborationController(CollaborationService collaborationService,
            SimpMessagingTemplate messagingTemplate) {
        this.collaborationService = collaborationService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/document/{docId}/insert")
    public void handleInsert(@DestinationVariable String docId,
                             @Payload InsertRequest request) {
        collaborationService.handleInsert(docId, request);
        broadcastDocumentState(docId);
    }

    @MessageMapping("/document/{docId}/delete")
    public void handleDelete(@DestinationVariable String docId,
                             @Payload DeleteRequest request) {
        collaborationService.handleDelete(docId, request);
        broadcastDocumentState(docId);
    }

    @MessageMapping("/document/{docId}/cursor")
    public void handleCursorUpdate(@DestinationVariable String docId,
                                   @Payload CursorUpdateRequest request) {
        collaborationService.handleCursorUpdate(docId, request);
        messagingTemplate.convertAndSend("/topic/document/" + docId + "/cursors", request);
    }

    @MessageMapping("/document/{docId}/undo")
    public void handleUndo(@DestinationVariable String docId,
                           @Payload UndoRedoRequest request) {
        collaborationService.handleUndo(docId, request.getUserId());
        broadcastDocumentState(docId);
    }

    @MessageMapping("/document/{docId}/redo")
    public void handleRedo(@DestinationVariable String docId,
                           @Payload UndoRedoRequest request) {
        collaborationService.handleRedo(docId, request.getUserId());
        broadcastDocumentState(docId);
    }
    private void broadcastDocumentState(String docId) {
        documentService.findById(docId).ifPresent(document -> {
            messagingTemplate.convertAndSend(
                    "/topic/document/" + docId + "/state",
                    new DocumentStateResponse(
                            document.getText(),
                            document.getAllCursors(),
                            document.getActiveUsers()
                    )
            );
        });
    }

    @MessageMapping("/document/connect")
    public void handleConnect(@Payload ConnectRequest request) {
        System.out.println("Connect request: user=" + request.getUserId() + ", shareCode=" + request.getShareCode());
    }
}
