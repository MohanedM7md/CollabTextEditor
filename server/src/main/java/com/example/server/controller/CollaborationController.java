package com.example.server.controller;
import com.example.server.dto.requests.*;
import com.example.server.dto.responses.CursorResponse;
import com.example.server.dto.responses.DocumentStateResponse;
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
        System.out.println("[CollaborationController] Initialized with collaborationService and messagingTemplate");
    }

    @MessageMapping("/document/{docId}/insert")
    public void handleInsert(@DestinationVariable String docId,
                             @Payload InsertRequest request) {
        System.out.println("\n=== INSERT REQUEST ===");
        System.out.println("Document ID: " + docId);
        System.out.println("User ID: " + request.getUserId());
        System.out.println("Character: '" + request.getValue()+ "'");
        System.out.println("Position: " + request.getPosition());

        DocumentStateResponse response =   collaborationService.handleInsert(docId, request);
        System.out.println("Sending response to /topic/document/" + docId + "/state");
        System.out.println("Response text length: " + response.getText().length());
        messagingTemplate.convertAndSend("/topic/document/" + docId + "/state", response);
    }

    @MessageMapping("/document/{docId}/delete")
    public void handleDelete(@DestinationVariable String docId,
                             @Payload DeleteRequest request) {
        DocumentStateResponse response =  collaborationService.handleDelete(docId, request);
        messagingTemplate.convertAndSend("/topic/document/" + docId + "/state", response);
    }

    @MessageMapping("/document/{docId}/cursor")
    public void handleCursorUpdate(@DestinationVariable String docId,
                                   @Payload CursorUpdateRequest request) {
        System.out.println("\n=== Cursor ===");
        System.out.println("Document ID: " + docId);
        System.out.println("User ID: " + request.getUserId());
        System.out.println("Position: " + request.getPosition());
        CursorResponse response =  collaborationService.handleCursorUpdate(docId, request);

        messagingTemplate.convertAndSend("/topic/document/" + docId + "/cursors", response);
    }

    @MessageMapping("/document/{docId}/undo")
    public void handleUndo(@DestinationVariable String docId,
                           @Payload UndoRequest request) {
        DocumentStateResponse response = collaborationService.handleUndo(docId, request);
        messagingTemplate.convertAndSend("/topic/document/" + docId + "/state", response);

    }

    @MessageMapping("/document/{docId}/redo")
    public void handleRedo(@DestinationVariable String docId,
                           @Payload UndoRequest request) {
        DocumentStateResponse response = collaborationService.handleRedo(docId, request);
        messagingTemplate.convertAndSend("/topic/document/" + docId + "/state", response);

    }

    @MessageMapping("/document/connect")
    public void handleConnect(@Payload ConnectRequest request) {
        System.out.println("Connect request: user=" + request.getUserId() + ", shareCode=" + request.getShareCode());
        messagingTemplate.convertAndSend("/topic/document/" + request.getShareCode() + "/user-joined",
                new ConnectRequest());
    }
}
