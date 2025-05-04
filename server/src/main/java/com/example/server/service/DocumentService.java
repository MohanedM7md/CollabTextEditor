package com.example.server.service;

import com.example.server.model.Document;
import com.example.server.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DocumentService {
    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public Document createDocument(String ownerId, String editorCode ,String viewerCode,String title) {
        Document doc = new Document(ownerId,editorCode,viewerCode,title);
        repository.save(doc);
        return doc;
    }

    public Optional<Document> findById(String docId) {
        return repository.findById(docId);
    }

    public void save(Document document) {
        repository.save(document);
    }

    public Optional<Document> findByShareCode(String shareCode) {
        return repository.findByEditorCode(shareCode)
                .or(() -> repository.findByViewerCode(shareCode));
    }
}