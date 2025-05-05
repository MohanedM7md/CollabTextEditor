package com.example.server.controller;

import com.example.server.dto.responses.ShareCodeResponse;
import com.example.server.model.Document;
import com.example.server.service.DocumentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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


    @GetMapping("/by-share-code/{shareCode}/{userId}")
    public ResponseEntity<ShareCodeResponse> getDocumentByShareCode(@PathVariable String shareCode, @PathVariable String userId) {

        return documentService.joinByShareCode(shareCode, userId)
                .map(pair -> ResponseEntity.ok(new ShareCodeResponse(pair.getLeft().getId(), pair.getRight())))
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importDocument(
            @RequestParam("userId") String userId,
            @RequestParam("title") String title,
            @RequestParam("editorCode") String editorCode,
            @RequestParam("viewerCode") String viewerCode,
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Create and save document with CRDT operations
            Document document = documentService.importDocument(
                    userId, title, editorCode, viewerCode, content);

            return ResponseEntity.ok(Map.of(
                    "id", document.getId(),
                    "title", document.getTitle()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Import failed: " + e.getMessage());
        }
    }

    private String generateRandomCode() {
        return UUID.randomUUID().toString().substring(0, 6);
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
