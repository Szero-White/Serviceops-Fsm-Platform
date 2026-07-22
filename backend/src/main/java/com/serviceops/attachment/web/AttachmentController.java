package com.serviceops.attachment.web;

import com.serviceops.attachment.application.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse upload(@RequestParam String referenceType,
                                     @RequestParam UUID referenceId,
                                     @RequestParam MultipartFile file) {
        return service.upload(referenceType, referenceId, file);
    }

    @GetMapping
    public List<AttachmentResponse> list(@RequestParam String referenceType, @RequestParam UUID referenceId) {
        return service.list(referenceType, referenceId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable UUID id) {
        var file = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }

    public record AttachmentResponse(UUID id, String originalFilename, String contentType, long fileSize,
                                     String referenceType, UUID referenceId, String uploadedBy, Instant createdAt) {
    }
}
