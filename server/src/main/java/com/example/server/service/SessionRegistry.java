package com.example.server.service;

import com.example.server.model.UserSession;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionRegistry {
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, String userId, String docId) {
        sessions.put(sessionId, new UserSession(sessionId, userId, docId));
    }

    public void unregisterSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public Optional<UserSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Collection<UserSession> getDocumentSessions(String docId) {
        return sessions.values().stream()
                .filter(s -> docId.equals(s.getDocumentId()))
                .toList();
    }
}