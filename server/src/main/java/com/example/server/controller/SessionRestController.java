package com.example.server.controller;

import com.example.server.model.Session;
import com.example.server.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionRestController {

    private final SessionService sessionService;

    @Autowired
    public SessionRestController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/create")
    public Session createSession() {
        return sessionService.createSession();
    }

    @GetMapping("/{code}")
    public Session getSession(@PathVariable String code) {
        return sessionService.getSessionByCode(code);
    }
}
