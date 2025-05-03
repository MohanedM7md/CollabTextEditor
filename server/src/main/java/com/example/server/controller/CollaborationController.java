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

    @MessageMapping("/document/{docId}/operation")
    public void handleOperation(@DestinationVariable String docId, @Payload OperationRequest request) {
        System.out.println("Received operation for doc: " + docId);
        Operation op = request.getOperation();
        if (op == null || request.getUserId() == null) {
            System.out.println("Invalid operation payload");
            return;
        }

        Operation applied = collaborationService.applyOperation(docId, request.getUserId(), op);
        messagingTemplate.convertAndSend("/topic/document/" + docId + "/updates", applied);
    }

    @MessageMapping("/document/{docId}/cursor")
    public void handleCursorUpdate(@DestinationVariable String docId, @Payload CursorUpdateRequest request) {
        System.out.println("Received cursor update for doc: " + docId);

        if (request.getUserId() == null) {
            System.out.println("No userId in cursor update");
            return;
        }

        boolean updated = collaborationService.updateCursor(
                docId,
                request.getUserId(),
                request.getPosition(),
                request.getColor());

        if (updated) {
            messagingTemplate.convertAndSend("/topic/document/" + docId + "/cursors", request);
        }
    }

    @MessageMapping("/document/connect")
    public void handleConnect(@Payload ConnectRequest request) {
        System.out.println("Connect request: user=" + request.getUserId() + ", shareCode=" + request.getShareCode());
    }
}
