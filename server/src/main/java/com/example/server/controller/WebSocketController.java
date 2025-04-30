package com.example.server.controller;

import com.example.server.model.Operation;
import com.example.server.model.User;
import com.example.server.service.CRDTService;
import com.example.server.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CRDTService crdtService;
    private final SessionService sessionService;

    @Autowired
    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                               CRDTService crdtService,
                               SessionService sessionService) {
        this.messagingTemplate = messagingTemplate;
        this.crdtService = crdtService;
        this.sessionService = sessionService;
    }

    @MessageMapping("/session/{sessionId}/edit")
    public void handleEdit(@DestinationVariable String sessionId,
                           @Payload Operation operation) {

        Operation transformed = crdtService.applyOperation(sessionId, operation);
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/edit", transformed);
    }

    @MessageMapping("/session/{sessionId}/line-update")
    public void handleLineUpdate(@DestinationVariable String sessionId,
                                 @Payload User user) {
        sessionService.updateUserLine(sessionId, user.getUserId(), user.getCurrentLine());
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/lines", user);
    }

}
