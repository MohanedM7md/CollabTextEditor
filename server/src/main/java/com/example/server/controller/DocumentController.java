package com.example.server.controller;

import com.example.server.model.Document;
import com.example.server.service.DocumentService;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }


    @PostMapping("/create")
    public Document createDocument(@RequestParam String userId) {
        return documentService.createDocument(userId);
    }

    @GetMapping("/{id}")
    public Optional<Document> getDocument(@PathVariable String id) {
        return documentService.findById(id);
    }
}