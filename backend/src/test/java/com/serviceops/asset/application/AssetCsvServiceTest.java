package com.serviceops.asset.application;

import com.serviceops.common.application.CsvFileService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AssetCsvServiceTest {

    @Test
    void templateUsesCanonicalCustomerBusinessCodeExample() {
        AssetCsvService service = new AssetCsvService(new CsvFileService());

        String csv = new String(service.assetTemplate(), StandardCharsets.UTF_8);

        assertThat(csv)
                .contains("KH-20260115-001")
                .doesNotContain("KH-0001");
    }
}
