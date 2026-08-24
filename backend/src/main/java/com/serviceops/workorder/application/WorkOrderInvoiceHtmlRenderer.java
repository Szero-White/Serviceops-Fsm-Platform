package com.serviceops.workorder.application;

import com.serviceops.inventory.domain.InventoryPartUsage;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class WorkOrderInvoiceHtmlRenderer {
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final String TEMPLATE_PATH = "templates/work-order-invoice.html";
    private final String invoiceTemplate = loadTemplate();

    public String render(WorkOrderResponse workOrder, List<InventoryPartUsage> consumedParts) {
        BigDecimal partsTotal = consumedParts.stream()
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String invoiceCode = "HD-" + workOrder.code();
        String issuedAt = DATE_TIME_FORMATTER.format(Instant.now());
        String createdAt = formatDateTime(workOrder.createdAt());
        String completedAt = workOrder.completedAt() == null ? "Chưa hoàn thành" : formatDateTime(workOrder.completedAt());

        return template().formatted(
                escape(invoiceCode),
                escape(invoiceCode),
                escape(workOrder.code()),
                escape(issuedAt),
                escape(statusLabel(workOrder.status().name())),
                escape(workOrder.customerName()),
                escape(workOrder.assetLabel() == null ? "Không có thiết bị" : workOrder.assetLabel()),
                escape(workOrder.summary()),
                escape(createdAt),
                escape(statusLabel(workOrder.status().name())),
                escape(completedAt),
                escape(workOrder.technicianName() == null ? "Chưa phân công" : workOrder.technicianName()),
                renderRows(consumedParts),
                formatMoney(partsTotal),
                formatMoney(partsTotal)
        );
    }

    private String template() {
        return invoiceTemplate;
    }

    private static String loadTemplate() {
        try {
            return new ClassPathResource(TEMPLATE_PATH).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load work order invoice template", exception);
        }
    }

    private String renderRows(List<InventoryPartUsage> consumedParts) {
        if (consumedParts.isEmpty()) {
            return """
                    <tr>
                      <td colspan="5" class="empty">Chưa có phụ tùng được ghi nhận cho phiếu này.</td>
                    </tr>
                    """;
        }

        StringBuilder rows = new StringBuilder();
        int index = 1;
        for (InventoryPartUsage usage : consumedParts) {
            rows.append("""
                    <tr>
                      <td>%d</td>
                      <td>
                        <div class="item-name">%s</div>
                        <div class="item-code">%s</div>
                      </td>
                      <td class="right">%s %s</td>
                      <td class="right">%s</td>
                      <td class="right"><strong>%s</strong></td>
                    </tr>
                    """.formatted(
                    index++,
                    escape(usage.sparePart().getName()),
                    escape(usage.sparePart().getSku()),
                    formatNumber(usage.quantity()),
                    escape(usage.sparePart().getUnit()),
                    formatMoney(usage.sparePart().getUnitPrice()),
                    formatMoney(amount(usage))
            ));
        }
        return rows.toString();
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "OPEN" -> "Đang mở";
            case "SCHEDULED" -> "Đã lên lịch";
            case "ASSIGNED" -> "Đã phân công";
            case "ON_THE_WAY" -> "Đang di chuyển";
            case "IN_PROGRESS" -> "Đang thực hiện";
            case "WAITING_FOR_PARTS" -> "Chờ phụ tùng";
            case "COMPLETED" -> "Đã hoàn thành";
            case "CUSTOMER_ACCEPTED" -> "Khách xác nhận";
            case "CLOSED" -> "Đã đóng";
            case "CANCELLED" -> "Đã hủy";
            case "REOPENED" -> "Mở lại";
            default -> status;
        };
    }

    private static String formatDateTime(Instant value) {
        return value == null ? "Không có dữ liệu" : DATE_TIME_FORMATTER.format(value);
    }

    private BigDecimal amount(InventoryPartUsage usage) {
        return usage.sparePart().getUnitPrice().multiply(usage.quantity()).setScale(0, RoundingMode.HALF_UP);
    }

    private static String formatMoney(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(VIETNAM);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(value == null ? BigDecimal.ZERO : value);
    }

    private static String formatNumber(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
