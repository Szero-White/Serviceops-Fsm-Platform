package com.serviceops.attachment.application;

import com.serviceops.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}
    );
    private static final byte[] WEBP_RIFF = new byte[]{0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MARKER = new byte[]{0x57, 0x45, 0x42, 0x50};

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
    public synchronized StoredFile store(MultipartFile file, String tenantFolder) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("EMPTY_FILE", "File tải lên không được rỗng");
        }

        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw BusinessException.badRequest("INVALID_FILE_TYPE", "Chỉ hỗ trợ JPG, PNG, WEBP và PDF");
        }
        validateFileSignature(file, contentType);

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> "";
        };

        String safeFolder = sanitizeRelativeFolder(tenantFolder);
        enforceTenantQuota(safeFolder, file.getSize());
        String storageKey = safeFolder + "/" + UUID.randomUUID() + extension;
        Path target = resolveStorageKey(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String originalName = safeOriginalFilename(file.getOriginalFilename(), extension);
            return new StoredFile(storageKey, originalName, contentType, file.getSize());
        } catch (IOException ex) {
            throw new BusinessException("FILE_STORAGE_ERROR", "Không thể lưu file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void enforceTenantQuota(String safeFolder, long incomingBytes) {
        long maxTenantBytes = properties.maxTenantBytes();
        if (maxTenantBytes <= 0) {
            return;
        }

        String tenantSegment = safeFolder.split("/", 2)[0];
        Path tenantRoot = resolveStorageKey(tenantSegment);
        long usedBytes = directorySize(tenantRoot);
        if (incomingBytes > maxTenantBytes || usedBytes > maxTenantBytes - incomingBytes) {
            throw new BusinessException(
                    "STORAGE_QUOTA_EXCEEDED",
                    "Dung lượng lưu trữ của tenant đã đạt giới hạn",
                    HttpStatus.PAYLOAD_TOO_LARGE
            );
        }
    }

    private static long directorySize(Path directory) {
        if (!Files.exists(directory)) {
            return 0L;
        }
        try (var paths = Files.walk(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException ex) {
                            throw new StorageSizeReadException(ex);
                        }
                    })
                    .sum();
        } catch (StorageSizeReadException | IOException ex) {
            throw new BusinessException(
                    "FILE_STORAGE_ERROR",
                    "Không thể kiểm tra dung lượng lưu trữ",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private static final class StorageSizeReadException extends RuntimeException {
        private StorageSizeReadException(IOException cause) {
            super(cause);
        }
    }

    @Override
    public Resource load(String storageKey) {
        try {
            Path path = resolveStorageKey(storageKey);
            if (!Files.isRegularFile(path)) {
                throw BusinessException.notFound("FILE_NOT_FOUND", "Không tìm thấy file");
            }
            return new UrlResource(path.toUri());
        } catch (IOException ex) {
            throw BusinessException.notFound("FILE_NOT_FOUND", "Không tìm thấy file");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveStorageKey(storageKey));
        } catch (IOException ex) {
            throw new BusinessException("FILE_DELETE_ERROR", "Không thể xóa file đính kèm", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw BusinessException.badRequest("INVALID_STORAGE_PATH", "Đường dẫn file không hợp lệ");
        }
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw BusinessException.badRequest("INVALID_STORAGE_PATH", "Đường dẫn file không hợp lệ");
        }
        return path;
    }

    private static void validateFileSignature(MultipartFile file, String contentType) {
        try (InputStream raw = file.getInputStream(); BufferedInputStream input = new BufferedInputStream(raw)) {
            byte[] header = input.readNBytes(12);
            boolean valid;
            if ("image/webp".equals(contentType)) {
                valid = header.length >= 12
                        && matchesAt(header, WEBP_RIFF, 0)
                        && matchesAt(header, WEBP_MARKER, 8);
            } else {
                byte[] signature = SIGNATURES.get(contentType);
                valid = signature != null && matchesAt(header, signature, 0);
            }
            if (!valid) {
                throw BusinessException.badRequest(
                        "INVALID_FILE_SIGNATURE",
                        "Nội dung file không khớp với định dạng đã khai báo"
                );
            }
        } catch (IOException ex) {
            throw new BusinessException("FILE_READ_ERROR", "Không thể đọc file tải lên", HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean matchesAt(byte[] content, byte[] signature, int offset) {
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[offset + index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private static String sanitizeRelativeFolder(String folder) {
        if (folder == null || folder.isBlank() || folder.startsWith("/") || folder.startsWith("\\")) {
            throw BusinessException.badRequest("INVALID_STORAGE_PATH", "Thư mục lưu file không hợp lệ");
        }
        if (folder.indexOf('\\') >= 0) {
            throw BusinessException.badRequest("INVALID_STORAGE_PATH", "Thư mục lưu file không hợp lệ");
        }

        String[] segments = folder.trim().split("/", -1);
        for (String segment : segments) {
            if (segment.isBlank() || !segment.matches("[A-Za-z0-9_-]+")) {
                throw BusinessException.badRequest("INVALID_STORAGE_PATH", "Thư mục lưu file không hợp lệ");
            }
        }
        return String.join("/", segments);
    }

    private static String safeOriginalFilename(String originalFilename, String fallbackExtension) {
        String fallback = "upload" + fallbackExtension;
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallback;
        }
        try {
            String filename = Path.of(originalFilename).getFileName().toString().trim();
            if (filename.isBlank() || filename.length() > 255 || filename.chars().anyMatch(Character::isISOControl)) {
                return fallback;
            }
            return filename;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }
}
