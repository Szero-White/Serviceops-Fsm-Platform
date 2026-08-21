package com.serviceops.customer.application;

import com.serviceops.common.application.CsvFileService;
import com.serviceops.customer.web.CustomerDtos.CustomerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerCsvService {
    private static final List<String> CUSTOMER_HEADERS = List.of(
            "code", "name", "phone", "email", "address", "notes", "active"
    );

    private final CsvFileService csvFileService;

    public List<CustomerCsvRow> parseCustomers(MultipartFile file) {
        return csvFileService.parse(file, CUSTOMER_HEADERS, "khách hàng")
                .stream()
                .map(row -> new CustomerCsvRow(
                        row.rowNumber(),
                        row.value(0),
                        row.value(1),
                        row.value(2),
                        row.value(3),
                        row.value(4),
                        row.value(5),
                        row.value(6)
                ))
                .toList();
    }

    public byte[] customerTemplate() {
        return csvFileService.write(List.of(
                CUSTOMER_HEADERS,
                List.of("KH-01001", "Công ty Minh Anh", "0909123456", "support@example.com", "12 Nguyễn Trãi, TP.HCM", "Khách hàng bảo trì định kỳ", "true")
        ));
    }

    public byte[] exportCustomers(List<CustomerResponse> customers) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("code", "name", "phone", "email", "address", "notes", "active", "createdAt", "updatedAt"));
        for (CustomerResponse customer : customers) {
            rows.add(List.of(
                    cell(customer.code()),
                    cell(customer.name()),
                    cell(customer.phone()),
                    cell(customer.email()),
                    cell(customer.address()),
                    cell(customer.notes()),
                    Boolean.toString(customer.active()),
                    customer.createdAt() == null ? "" : customer.createdAt().toString(),
                    customer.updatedAt() == null ? "" : customer.updatedAt().toString()
            ));
        }
        return csvFileService.write(rows);
    }

    private static String cell(String value) {
        return value == null ? "" : value;
    }

    public record CustomerCsvRow(
            int rowNumber,
            String code,
            String name,
            String phone,
            String email,
            String address,
            String notes,
            String active
    ) {
    }
}
