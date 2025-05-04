package com.example.server.controller;

import com.example.server.model.Document;
import com.example.server.service.DocumentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/create")
    public Document createDocument(@RequestParam String userId,@RequestParam String editorcode,@RequestParam String viewercode,@RequestParam String title) {
        return documentService.createDocument(userId,editorcode,viewercode,title);

    }


    @GetMapping("/titles")
    public List<String> getDocumentTitles() {
        System.out.println("getDocumentTitles");
        return documentService.getAllDocumentTitles();
    }

    @GetMapping("/{id}")
    public Optional<Document> getDocument(@PathVariable String id) {
        System.out.println("getDocument: "+id);
        return documentService.findById(id);
    }


    @GetMapping("/by-share-code/{shareCode}")
    public ResponseEntity<Document> getDocumentByShareCode(@PathVariable String shareCode) {
        System.out.println("getDocumentByShareCode: "+shareCode);
        return documentService.findByShareCode(shareCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping("/{id}/import")
    public ResponseEntity<String> importText(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId
    ) {
        try {
            Document document = documentService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            File tempFile = new File("crdt_single_thread_test.txt");
            file.transferTo(tempFile);

            document.importFromTextFile(tempFile, userId);
            document.updateTimestamp();
            documentService.save(document);
            return ResponseEntity.ok("Import successful.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File I/O error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Import failed: " + e.getMessage());
        }
    }


    @GetMapping("/{id}/export")
    public ResponseEntity<InputStreamResource> exportText(@PathVariable String id) {
        try {
            Document document = documentService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            File tempFile = new File("crdt99.txt");
            document.exportToTextFile(tempFile);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + id + ".txt")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(tempFile.length())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
