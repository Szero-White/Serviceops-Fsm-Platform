package com.serviceops.payment.application;

import com.serviceops.payment.domain.PaymentMethod;
import com.serviceops.payment.domain.PaymentReceipt;
import com.serviceops.workorder.domain.WorkOrderBillingItem;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshot;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class PaymentReceiptHtmlRenderer {
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final String TEMPLATE_PATH = "templates/payment-receipt.html";
    private final String template = loadTemplate();

    public String render(
            PaymentReceipt receipt,
            WorkOrderBillingSnapshot billing,
            List<WorkOrderBillingItem> items
    ) {
        return template.formatted(
                escape(receipt.getReceiptCode()),
                escape(receipt.getReceiptCode()),
                escape(receipt.getWorkOrderCodeSnapshot()),
                escape(formatDateTime(receipt.getIssuedAt())),
                escape(receipt.getCustomerNameSnapshot()),
                escape(paymentMethodLabel(receipt.getPaymentMethod())),
                escape(formatDateTime(receipt.getSettledAt())),
                escape(receipt.getSettledByDisplayName()),
                renderRows(items),
                escape(billing.getIncidentalReason() == null ? "Không có" : billing.getIncidentalReason()),
                formatMoney(billing.getPartsTotal()),
                formatMoney(billing.getLaborFee()),
                formatMoney(billing.getIncidentalFee()),
                formatMoney(receipt.getAmount()),
                escape(receipt.getIssuedByDisplayName())
        );
    }

    private static String loadTemplate() {
        try {
            return new ClassPathResource(TEMPLATE_PATH).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load payment receipt template", exception);
        }
    }

    private static String renderRows(List<WorkOrderBillingItem> items) {
        if (items.isEmpty()) {
            return """
                    <tr>
                      <td colspan="5" class="empty">Không có phụ tùng tính phí.</td>
                    </tr>
                    """;
        }
        StringBuilder rows = new StringBuilder();
        int index = 1;
        for (WorkOrderBillingItem item : items) {
            rows.append("""
                    <tr>
                      <td>%d</td>
                      <td><strong>%s</strong><div class="muted">%s</div></td>
                      <td class="right">%s %s</td>
                      <td class="right">%s</td>
                      <td class="right"><strong>%s</strong></td>
                    </tr>
                    """.formatted(
                    index++,
                    escape(item.getSparePartName()),
                    escape(item.getSparePartSku()),
                    formatQuantity(item.getQuantity()),
                    escape(item.getUnit()),
                    formatMoney(item.getUnitPrice()),
                    formatMoney(item.getLineTotal())
            ));
        }
        return rows.toString();
    }

    private static String paymentMethodLabel(PaymentMethod method) {
        return method == PaymentMethod.BANK_TRANSFER ? "Chuyển khoản" : "Tiền mặt";
    }

    private static String formatDateTime(Instant value) {
        return value == null ? "Không có dữ liệu" : DATE_TIME.format(value);
    }

    private static String formatMoney(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(VIETNAM);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(value == null ? BigDecimal.ZERO : value);
    }

    private static String formatQuantity(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
