package com.serviceops.workorder.application;

import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WorkOrderInvoiceService {
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final InventoryTransactionRepository transactionRepository;

    public byte[] exportInvoice(WorkOrderResponse workOrder) {
        List<InventoryTransaction> consumedParts = transactionRepository.findConsumedPartsForWorkOrder(CurrentUser.tenantId(), workOrder.id());
        return render(workOrder, consumedParts).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String render(WorkOrderResponse workOrder, List<InventoryTransaction> consumedParts) {
        BigDecimal partsTotal = consumedParts.stream()
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder rows = new StringBuilder();
        if (consumedParts.isEmpty()) {
            rows.append("""
                    <tr>
                      <td colspan="5" class="empty">Chua co phu tung duoc ghi nhan cho phieu nay.</td>
                    </tr>
                    """);
        } else {
            int index = 1;
            for (InventoryTransaction tx : consumedParts) {
                rows.append("""
                        <tr>
                          <td>%d</td>
                          <td><strong>%s</strong><br><span>%s</span></td>
                          <td class="right">%s %s</td>
                          <td class="right">%s</td>
                          <td class="right">%s</td>
                        </tr>
                        """.formatted(
                        index++,
                        escape(tx.getSparePart().getName()),
                        escape(tx.getSparePart().getSku()),
                        formatNumber(tx.getQuantity()),
                        escape(tx.getSparePart().getUnit()),
                        formatMoney(tx.getSparePart().getUnitPrice()),
                        formatMoney(amount(tx))
                ));
            }
        }

        String createdAt = workOrder.createdAt() == null ? "" : DATE_TIME_FORMATTER.format(workOrder.createdAt());
        String completedAt = workOrder.completedAt() == null ? "Chua hoan thanh" : DATE_TIME_FORMATTER.format(workOrder.completedAt());

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <title>Hoa don %s</title>
                  <style>
                    body { font-family: Arial, sans-serif; color: #0f172a; margin: 40px; }
                    .top { display: flex; justify-content: space-between; gap: 24px; border-bottom: 2px solid #1d4ed8; padding-bottom: 20px; }
                    .brand { font-size: 24px; font-weight: 800; color: #1d4ed8; }
                    .muted, span { color: #64748b; }
                    h1 { margin: 28px 0 8px; font-size: 28px; }
                    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 24px 0; }
                    .box { border: 1px solid #dbe4f0; border-radius: 12px; padding: 16px; }
                    .label { color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: .04em; margin-bottom: 6px; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 18px; }
                    th { text-align: left; background: #f8fafc; color: #475569; font-size: 12px; text-transform: uppercase; }
                    th, td { border-bottom: 1px solid #e2e8f0; padding: 12px; vertical-align: top; }
                    .right { text-align: right; }
                    .empty { text-align: center; color: #64748b; padding: 28px; }
                    .total { margin-top: 24px; display: flex; justify-content: flex-end; }
                    .total-box { min-width: 280px; border: 1px solid #dbe4f0; border-radius: 12px; padding: 16px; }
                    .total-row { display: flex; justify-content: space-between; margin: 8px 0; }
                    .grand { font-size: 20px; font-weight: 800; color: #1d4ed8; }
                    @media print { body { margin: 20px; } }
                  </style>
                </head>
                <body>
                  <section class="top">
                    <div>
                      <div class="brand">ServiceOps</div>
                      <div class="muted">Field Service Platform</div>
                    </div>
                    <div class="right">
                      <strong>%s</strong><br>
                      <span>Ngay tao phieu: %s</span>
                    </div>
                  </section>

                  <h1>Hoa don dich vu</h1>
                  <div class="muted">Ban co the in trang nay hoac luu thanh PDF tu trinh duyet.</div>

                  <section class="grid">
                    <div class="box">
                      <div class="label">Khach hang</div>
                      <strong>%s</strong><br>
                      <span>%s</span>
                    </div>
                    <div class="box">
                      <div class="label">Cong viec</div>
                      <strong>%s</strong><br>
                      <span>Trang thai: %s - Hoan thanh: %s</span>
                    </div>
                  </section>

                  <table>
                    <thead>
                      <tr>
                        <th style="width: 52px;">#</th>
                        <th>Hang muc</th>
                        <th class="right">So luong</th>
                        <th class="right">Don gia</th>
                        <th class="right">Thanh tien</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                  </table>

                  <section class="total">
                    <div class="total-box">
                      <div class="total-row"><span>Tong phu tung</span><strong>%s</strong></div>
                      <div class="total-row"><span>Phi dich vu</span><strong>Nhap khi xuat hoa don chinh thuc</strong></div>
                      <div class="total-row grand"><span>Tam tinh</span><strong>%s</strong></div>
                    </div>
                  </section>
                </body>
                </html>
                """.formatted(
                escape(workOrder.code()),
                escape(workOrder.code()),
                escape(createdAt),
                escape(workOrder.customerName()),
                escape(workOrder.assetLabel() == null ? "Khong co thiet bi" : workOrder.assetLabel()),
                escape(workOrder.summary()),
                escape(workOrder.status().name()),
                escape(completedAt),
                rows,
                formatMoney(partsTotal),
                formatMoney(partsTotal)
        );
    }

    private BigDecimal amount(InventoryTransaction tx) {
        return tx.getSparePart().getUnitPrice().multiply(tx.getQuantity()).setScale(0, RoundingMode.HALF_UP);
    }

    private static String formatMoney(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(VIETNAM).format(value == null ? BigDecimal.ZERO : value);
    }

    private static String formatNumber(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
