package com.example.server.service;

import com.example.server.model.Document;
import com.example.server.repository.DocumentRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {
    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public Document createDocument(String ownerId, String editorCode ,String viewerCode,String title) {
        Document doc = new Document(ownerId,editorCode,viewerCode,title);
        repository.save(doc);
        System.out.println("createDocument: " + doc.getId());
        System.out.println("Document saved: " + doc);
        return doc;
    }

    public Optional<Document> findById(String docId) {
        return repository.findById(docId);
    }

    public void save(Document document) {
        System.out.println("Saved document: " + document);
        repository.save(document);
    }

    public Optional<Pair<Document, String>> joinByShareCode(String shareCode,String userId) {
        Optional<Document> docOpt = repository.findByViewerCode(shareCode);
        if (docOpt.isPresent()) {
            Document doc = docOpt.get();
            doc.addViewer(userId);
            return Optional.of(Pair.of(doc, "viewer"));
        }

        docOpt = repository.findByEditorCode(shareCode);
        if (docOpt.isPresent()) {
            Document doc = docOpt.get();
            doc.addEditor(userId);
            return Optional.of(Pair.of(doc, "editor"));
        }

        return Optional.empty();
    }
    public List<String> getAllDocumentTitles() {
        return repository.findAll()
                .stream()
                .map(Document::getTitle)
                .collect(Collectors.toList());
    }
}