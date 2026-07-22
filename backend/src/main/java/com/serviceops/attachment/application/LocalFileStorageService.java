package com.serviceops.attachment.application;

import com.serviceops.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private final StorageProperties properties;
    private Path root;

    @PostConstruct
    void initialize() {
        try {
            root = Path.of(properties.root()).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot initialize local file storage", ex);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String tenantFolder) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("EMPTY_FILE", "File tải lên không được rỗng");
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw BusinessException.badRequest("INVALID_FILE_TYPE", "Chỉ hỗ trợ JPG, PNG, WEBP và PDF");
        }
        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
        String storageKey = tenantFolder + "/" + UUID.randomUUID() + extension;
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("INVALID_STORAGE_PATH", "Đường dẫn lưu file không hợp lệ", HttpStatus.BAD_REQUEST);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String originalName = Path.of(file.getOriginalFilename() == null ? "upload" + extension : file.getOriginalFilename()).getFileName().toString();
            return new StoredFile(storageKey, originalName, contentType, file.getSize());
        } catch (IOException ex) {
            throw new BusinessException("FILE_STORAGE_ERROR", "Không thể lưu file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Resource load(String storageKey) {
        try {
            Path path = root.resolve(storageKey).normalize();
            if (!path.startsWith(root) || !Files.exists(path)) {
                throw BusinessException.notFound("FILE_NOT_FOUND", "Không tìm thấy file");
            }
            return new UrlResource(path.toUri());
        } catch (IOException ex) {
            throw BusinessException.notFound("FILE_NOT_FOUND", "Không tìm thấy file");
        }
    }
}
