package com.example.server.controller;

import com.example.server.CRDT.operations.Operation;
import com.example.server.dto.requests.ConnectRequest;
import com.example.server.dto.requests.CursorUpdateRequest;
import com.example.server.dto.requests.OperationRequest;
import com.example.server.service.CollaborationService;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;

@Controller
public class CollaborationController {
    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @MessageMapping("/document/{docId}/operation")
    public void handleOperation(
            @Payload OperationRequest request,
            @DestinationVariable String docId,
            @Header("simpSessionId") String sessionId) {

        collaborationService.applyOperation(
                docId,
                request.getUserId(),
                request.getOperation()
        );
    }

    @MessageMapping("/document/{docId}/cursor")
    public void handleCursorUpdate(
            @Payload CursorUpdateRequest request,
            @DestinationVariable String docId,
            @Header("simpSessionId") String sessionId) {

        System.out.println("Update el cursor");
        /*collaborationService.updateCursor(
                sessionId,
                docId,
                request.getUserId(),
                request.getPosition(),
                request.getColor()
        );*/
    }

    @MessageMapping("/document/connect")
    public void handleConnect(@Payload ConnectRequest request) {
        // request.getUserId()
        // request.getShareCode()
    }


}

