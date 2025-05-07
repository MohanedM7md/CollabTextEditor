package com.example.server.repository;

import com.example.server.model.Document;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface DocumentRepository {
    Optional<Document> findById(String id);
    Optional<Document> findByEditorCode(String code);
    Optional<Document> findByViewerCode(String code);
    List<Document> findAll();  // This will return a list of all documents
    void save(Document document);
}

