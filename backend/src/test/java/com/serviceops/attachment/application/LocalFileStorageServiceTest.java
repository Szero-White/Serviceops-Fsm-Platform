package com.serviceops.attachment.application;

import com.serviceops.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptsPngWhenDeclaredTypeMatchesMagicBytesAndNestedFolderIsSafe() {
        LocalFileStorageService service = service();
        MockMultipartFile file = file("proof.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3});

        FileStorageService.StoredFile stored = service.store(file, "tenant-1/work_order");

        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.storageKey()).startsWith("tenant-1/work_order/").endsWith(".png");
        assertThat(tempDir.resolve(stored.storageKey())).isRegularFile();
    }

    @Test
    void acceptsJpegPdfAndWebpSignatures() {
        LocalFileStorageService service = service();

        var jpeg = service.store(file("photo.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2}), "tenant/jpg");
        var pdf = service.store(file("proof.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 1, 2}), "tenant/pdf");
        var webp = service.store(file("photo.webp", "image/webp",
                new byte[]{0x52, 0x49, 0x46, 0x46, 1, 2, 3, 4, 0x57, 0x45, 0x42, 0x50, 1}), "tenant/webp");

        assertThat(jpeg.storageKey()).endsWith(".jpg");
        assertThat(pdf.storageKey()).endsWith(".pdf");
        assertThat(webp.storageKey()).endsWith(".webp");
    }

    @Test
    void rejectsExecutableContentDisguisedAsPng() {
        LocalFileStorageService service = service();
        MockMultipartFile file = file("fake.png", "image/png", new byte[]{0x4D, 0x5A, 1, 2, 3});

        assertThatThrownBy(() -> service.store(file, "tenant-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không khớp");
    }

    @Test
    void rejectsUnsupportedContentType() {
        LocalFileStorageService service = service();
        MockMultipartFile file = file("payload.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> service.store(file, "tenant-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPG");
    }

    @Test
    void rejectsUnsafeRelativeTraversalFolder() {
        LocalFileStorageService service = service();
        MockMultipartFile file = validPdf();

        assertThatThrownBy(() -> service.store(file, "../other-tenant"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.store(file, "tenant/../other"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.store(file, "tenant//asset"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsAbsoluteAndBackslashStorageFolders() {
        LocalFileStorageService service = service();
        MockMultipartFile file = validPdf();

        assertThatThrownBy(() -> service.store(file, "/tmp/escape"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.store(file, "tenant\\..\\escape"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void loadAndDeleteStayInsideConfiguredRoot() throws Exception {
        LocalFileStorageService service = service();
        var stored = service.store(validPdf(), "tenant/service_request");

        assertThat(service.load(stored.storageKey()).exists()).isTrue();

        service.delete(stored.storageKey());

        assertThat(Files.exists(tempDir.resolve(stored.storageKey()))).isFalse();
        assertThatThrownBy(() -> service.load(stored.storageKey()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void loadAndDeleteRejectTraversalKeys() {
        LocalFileStorageService service = service();

        assertThatThrownBy(() -> service.load("../outside.pdf"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete("../outside.pdf"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unsafeOriginalFilenameFallsBackWithoutAffectingStoragePath() {
        LocalFileStorageService service = service();
        MockMultipartFile file = file("../proof.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 1, 2});

        var stored = service.store(file, "tenant/reference");

        assertThat(stored.originalFilename()).isEqualTo("proof.pdf");
        assertThat(stored.storageKey()).startsWith("tenant/reference/");
    }


    @Test
    void enforcesConfiguredTenantQuotaWithoutAffectingOtherTenants() {
        LocalFileStorageService service = new LocalFileStorageService(new StorageProperties(tempDir.toString(), 10));
        service.initialize();

        service.store(validPdf(), "tenant-a/work_order");

        assertThatThrownBy(() -> service.store(validPdf(), "tenant-a/asset"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("STORAGE_QUOTA_EXCEEDED"));

        assertThat(service.store(validPdf(), "tenant-b/work_order").storageKey())
                .startsWith("tenant-b/work_order/");
    }

    private LocalFileStorageService service() {
        LocalFileStorageService service = new LocalFileStorageService(new StorageProperties(tempDir.toString(), 0));
        service.initialize();
        return service;
    }

    private static MockMultipartFile validPdf() {
        return file("proof.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 1, 2});
    }

    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }
}
