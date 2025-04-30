package com.example.server.service;

import com.example.server.model.Operation;
import com.example.server.model.Session;
import com.example.server.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session createSession() {
        String sessionId = UUID.randomUUID().toString();
        String editorCode = generateCode();
        String viewerCode = generateCode();

        Session session = new Session(sessionId, editorCode, viewerCode);
        sessions.put(sessionId, session);
        return session;
    }

    public Session getSessionByCode(String code) {
        return sessions.values().stream()
                .filter(s -> s.getEditorCode().equals(code) || s.getViewerCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Session not found for code: " + code));
    }

    public void updateUserLine(String sessionId, String userId, int line) {
        Session session = sessions.get(sessionId);
        if (session == null) return;

        for (User user : session.getActiveUsers()) {
            if (user.getUserId().equals(userId)) {
                user.setCurrentLine(line);
                return;
            }
        }
    }

    public void addUser(String sessionId, User user) {
        sessions.get(sessionId).getActiveUsers().add(user);
    }

    public List<User> getActiveUsers(String sessionId) {
        return sessions.getOrDefault(sessionId, new Session()).getActiveUsers();
    }

    public void saveOperation(String sessionId, Operation op) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.getDocumentOps().add(op);
        }
    }

    public List<Operation> getDocumentOperations(String sessionId) {
        return sessions.getOrDefault(sessionId, new Session()).getDocumentOps();
    }

    private String generateCode() {
        return "#" + UUID.randomUUID().toString().substring(0, 5);
    }
}
