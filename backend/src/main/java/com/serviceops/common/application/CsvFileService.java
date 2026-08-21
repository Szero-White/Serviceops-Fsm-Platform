package com.serviceops.common.application;

import com.serviceops.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvFileService {
    private static final int MAX_IMPORT_ROWS = 1_000;
    private static final String UTF8_BOM = "\uFEFF";

    public List<CsvRow> parse(MultipartFile file, List<String> expectedHeaders, String domainLabel) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("IMPORT_FILE_EMPTY", "File import không được để trống");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw BusinessException.badRequest("IMPORT_FILE_EMPTY", "File import không có dữ liệu");
            }

            List<String> headers = parseLine(removeBom(headerLine));
            if (!headers.equals(expectedHeaders)) {
                throw BusinessException.badRequest("IMPORT_HEADER_INVALID", "File import không đúng mẫu cột của " + domainLabel);
            }

            List<CsvRow> rows = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                if (rows.size() >= MAX_IMPORT_ROWS) {
                    throw BusinessException.badRequest("IMPORT_TOO_MANY_ROWS", "Mỗi lần chỉ import tối đa 1000 dòng");
                }
                rows.add(new CsvRow(rowNumber, parseLine(line)));
            }
            return rows;
        } catch (IOException ex) {
            throw new BusinessException("IMPORT_FILE_READ_ERROR", "Không thể đọc file import", HttpStatus.BAD_REQUEST);
        }
    }

    public byte[] write(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder(UTF8_BOM);
        for (List<String> row : rows) {
            builder.append(row.stream().map(CsvFileService::escape).reduce((left, right) -> left + "," + right).orElse(""));
            builder.append("\r\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String escape(String value) {
        String normalized = value == null ? "" : value;
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n") || normalized.contains("\r")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private static String removeBom(String value) {
        return value.startsWith(UTF8_BOM) ? value.substring(1) : value;
    }

    public record CsvRow(int rowNumber, List<String> values) {
        public String value(int index) {
            return index < values.size() ? values.get(index).trim() : "";
        }
    }
}
