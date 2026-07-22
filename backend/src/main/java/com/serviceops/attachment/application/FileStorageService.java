package com.serviceops.attachment.application;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(MultipartFile file, String tenantFolder);
    Resource load(String storageKey);
    record StoredFile(String storageKey, String originalFilename, String contentType, long size) {}
}
