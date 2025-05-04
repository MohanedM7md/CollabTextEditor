package com.example.server.repository;

import com.example.server.model.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDocumentRepository implements DocumentRepository {
    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final Map<String, String> editorCodes = new ConcurrentHashMap<>();
    private final Map<String, String> viewerCodes = new ConcurrentHashMap<>();

    @Override
    public Optional<Document> findById(String id) {
        return Optional.ofNullable(documents.get(id));
    }

    @Override
    public Optional<Document> findByEditorCode(String code) {
        return Optional.ofNullable(editorCodes.get(code))
                .flatMap(this::findById);
    }

    @Override
    public Optional<Document> findByViewerCode(String code) {
        return Optional.ofNullable(viewerCodes.get(code))
                .flatMap(this::findById);
    }

    @Override
    public void save(Document document) {
        documents.put(document.getId(), document);
        editorCodes.put(document.getEditorCode(), document.getId());
        viewerCodes.put(document.getViewerCode(), document.getId());
    }
    @Override
    public List<Document> findAll() {
        System.out.println("Documents in memory: " + documents.size());
        return new ArrayList<>(documents.values());
    }
}
