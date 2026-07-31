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
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
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
        return render(workOrder, consumedParts).getBytes(StandardCharsets.UTF_8);
    }

    private String render(WorkOrderResponse workOrder, List<InventoryTransaction> consumedParts) {
        BigDecimal partsTotal = consumedParts.stream()
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String invoiceCode = "HD-" + workOrder.code();
        String issuedAt = DATE_TIME_FORMATTER.format(Instant.now());
        String createdAt = formatDateTime(workOrder.createdAt());
        String completedAt = workOrder.completedAt() == null ? "Chưa hoàn thành" : formatDateTime(workOrder.completedAt());

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <title>%s</title>
                  <style>
                    :root {
                      --ink: #0f172a;
                      --muted: #64748b;
                      --line: #e2e8f0;
                      --soft: #f8fafc;
                      --blue: #3b82f6;
                      --blue-2: #dbeafe;
                      --purple: #8b5cf6;
                      --purple-2: #f5f3ff;
                      --green: #10b981;
                      --green-2: #d1fae5;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      background: linear-gradient(135deg, #f8fafc 0%%, #e2e8f0 100%%);
                      color: var(--ink);
                      font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                      -webkit-font-smoothing: antialiased;
                      -moz-osx-font-smoothing: grayscale;
                    }
                    .page {
                      width: min(1100px, calc(100%% - 48px));
                      margin: 32px auto;
                      background: #fff;
                      border: 1px solid var(--line);
                      border-radius: 20px;
                      box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08), 0 2px 8px rgba(15, 23, 42, 0.04);
                      overflow: hidden;
                    }
                    .hero {
                      display: grid;
                      grid-template-columns: 1.2fr .8fr;
                      gap: 32px;
                      padding: 40px 48px;
                      background: linear-gradient(135deg, #1e293b 0%%, #3b82f6 50%%, #8b5cf6 100%%);
                      color: #fff;
                      position: relative;
                    }
                    .hero::before {
                      content: '';
                      position: absolute;
                      top: 0;
                      left: 0;
                      right: 0;
                      bottom: 0;
                      background: radial-gradient(circle at 20%% 30%%, rgba(255,255,255,0.1) 0%%, transparent 50%%);
                      pointer-events: none;
                    }
                    .brand { font-size: 32px; font-weight: 800; letter-spacing: -0.5px; position: relative; }
                    .brand-subtitle { margin-top: 6px; color: rgba(255,255,255,.85); font-size: 14px; font-weight: 500; position: relative; }
                    .doc-title { margin: 40px 0 0; font-size: 42px; font-weight: 800; letter-spacing: -1px; position: relative; }
                    .doc-note { margin-top: 10px; color: rgba(255,255,255,.80); line-height: 1.6; font-size: 14px; position: relative; }
                    .hero-card {
                      align-self: start;
                      border: 1px solid rgba(255,255,255,.20);
                      border-radius: 16px;
                      padding: 24px;
                      background: rgba(255,255,255,.12);
                      backdrop-filter: blur(12px);
                      box-shadow: 0 8px 24px rgba(0,0,0,0.15);
                      position: relative;
                    }
                    .hero-row { display: flex; justify-content: space-between; gap: 16px; padding: 10px 0; position: relative; }
                    .hero-label { color: rgba(255,255,255,.70); font-size: 13px; font-weight: 500; }
                    .hero-value { font-weight: 700; text-align: right; font-size: 15px; }
                    .content { padding: 40px 48px 48px; }
                    .section-title {
                      margin: 0 0 20px;
                      font-size: 13px;
                      color: #334155;
                      letter-spacing: 0.1em;
                      text-transform: uppercase;
                      font-weight: 700;
                    }
                    .summary-grid {
                      display: grid;
                      grid-template-columns: repeat(3, 1fr);
                      gap: 16px;
                      margin-bottom: 32px;
                    }
                    .info-box {
                      border: 1px solid var(--line);
                      border-radius: 16px;
                      padding: 20px;
                      background: linear-gradient(135deg, #ffffff 0%%, #f8fafc 100%%);
                      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
                      transition: all 0.3s ease;
                    }
                    .info-box:hover {
                      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
                    }
                    .label {
                      margin-bottom: 10px;
                      color: var(--muted);
                      font-size: 11px;
                      font-weight: 700;
                      text-transform: uppercase;
                      letter-spacing: 0.08em;
                    }
                    .primary { font-size: 18px; font-weight: 700; line-height: 1.4; color: var(--ink); }
                    .secondary { margin-top: 8px; color: var(--muted); line-height: 1.5; font-size: 13px; }
                    .status {
                      display: inline-flex;
                      align-items: center;
                      margin-top: 12px;
                      padding: 8px 14px;
                      border-radius: 999px;
                      background: linear-gradient(135deg, var(--blue-2) 0%%, var(--purple-2) 100%%);
                      color: var(--blue);
                      font-size: 12px;
                      font-weight: 700;
                      letter-spacing: 0.05em;
                    }
                    table {
                      width: 100%%;
                      border-collapse: separate;
                      border-spacing: 0;
                      overflow: hidden;
                      border: 1px solid var(--line);
                      border-radius: 16px;
                      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
                    }
                    th {
                      padding: 16px 20px;
                      background: linear-gradient(135deg, #f8fafc 0%%, #f1f5f9 100%%);
                      color: #334155;
                      font-size: 12px;
                      letter-spacing: 0.08em;
                      text-align: left;
                      text-transform: uppercase;
                      font-weight: 700;
                      border-bottom: 2px solid var(--line);
                    }
                    td {
                      padding: 18px 20px;
                      border-top: 1px solid #f1f5f9;
                      vertical-align: top;
                      font-size: 14px;
                    }
                    .right { text-align: right; }
                    .item-name { font-weight: 700; color: var(--ink); font-size: 15px; }
                    .item-code { margin-top: 6px; color: var(--muted); font-size: 13px; }
                    .empty {
                      padding: 40px 20px;
                      color: var(--muted);
                      text-align: center;
                      background: #fff;
                      font-size: 14px;
                    }
                    .totals {
                      display: grid;
                      grid-template-columns: 1fr 380px;
                      gap: 28px;
                      align-items: start;
                      margin-top: 32px;
                    }
                    .notice {
                      border: 1px dashed #cbd5e1;
                      border-radius: 16px;
                      padding: 20px;
                      color: var(--muted);
                      line-height: 1.6;
                      background: linear-gradient(135deg, #f8fafc 0%%, #ffffff 100%%);
                      font-size: 13px;
                    }
                    .total-box {
                      border: 1px solid var(--line);
                      border-radius: 16px;
                      padding: 24px;
                      background: linear-gradient(135deg, #ffffff 0%%, #f8fafc 100%%);
                      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06);
                    }
                    .total-row {
                      display: grid;
                      grid-template-columns: 1fr auto;
                      gap: 20px;
                      padding: 12px 0;
                      color: #475569;
                      font-size: 14px;
                    }
                    .total-row + .total-row { border-top: 1px solid #f1f5f9; }
                    .grand {
                      color: var(--ink);
                      font-size: 24px;
                      font-weight: 800;
                      margin-top: 4px;
                    }
                    .grand strong { 
                      background: linear-gradient(135deg, var(--blue) 0%%, var(--purple) 100%%);
                      -webkit-background-clip: text;
                      -webkit-text-fill-color: transparent;
                      background-clip: text;
                    }
                    .footer {
                      display: grid;
                      grid-template-columns: 1fr 1fr;
                      gap: 32px;
                      margin-top: 48px;
                    }
                    .signature {
                      min-height: 140px;
                      border-top: 2px solid var(--line);
                      padding-top: 20px;
                      text-align: center;
                      color: var(--muted);
                      font-size: 14px;
                      font-weight: 500;
                    }
                    @media print {
                      body { background: #fff; }
                      .page { width: 100%%; margin: 0; border: 0; border-radius: 0; box-shadow: none; }
                      .hero { print-color-adjust: exact; -webkit-print-color-adjust: exact; }
                    }
                    @media (max-width: 760px) {
                      .page { width: 100%%; margin: 0; border-radius: 0; }
                      .hero, .summary-grid, .totals, .footer { grid-template-columns: 1fr; }
                      .hero, .content { padding: 24px; }
                    }
                  </style>
                </head>
                <body>
                  <main class="page">
                    <section class="hero">
                      <div>
                        <div class="brand">ServiceOps</div>
                        <div class="brand-subtitle">Nền tảng quản lý dịch vụ hiện trường</div>
                        <div class="doc-title">Hóa đơn dịch vụ</div>
                        <div class="doc-note">Chứng từ tạm tính dựa trên phiếu công việc và phụ tùng đã ghi nhận trong hệ thống.</div>
                      </div>
                      <div class="hero-card">
                        <div class="hero-row"><span class="hero-label">Số hóa đơn</span><span class="hero-value">%s</span></div>
                        <div class="hero-row"><span class="hero-label">Mã phiếu</span><span class="hero-value">%s</span></div>
                        <div class="hero-row"><span class="hero-label">Ngày xuất</span><span class="hero-value">%s</span></div>
                        <div class="hero-row"><span class="hero-label">Trạng thái</span><span class="hero-value">%s</span></div>
                      </div>
                    </section>

                    <section class="content">
                      <div class="summary-grid">
                        <div class="info-box">
                          <div class="label">Khách hàng</div>
                          <div class="primary">%s</div>
                          <div class="secondary">%s</div>
                        </div>
                        <div class="info-box">
                          <div class="label">Công việc</div>
                          <div class="primary">%s</div>
                          <div class="secondary">Tạo lúc: %s</div>
                          <span class="status">%s</span>
                        </div>
                        <div class="info-box">
                          <div class="label">Hoàn thành</div>
                          <div class="primary">%s</div>
                          <div class="secondary">Kỹ thuật viên: %s</div>
                        </div>
                      </div>

                      <h2 class="section-title">Chi tiết hạng mục</h2>
                      <table>
                        <thead>
                          <tr>
                            <th style="width: 58px;">STT</th>
                            <th>Hạng mục</th>
                            <th class="right">Số lượng</th>
                            <th class="right">Đơn giá</th>
                            <th class="right">Thành tiền</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                        </tbody>
                      </table>

                      <section class="totals">
                        <div class="notice">
                          Phí dịch vụ, thuế VAT hoặc chiết khấu chưa được cấu hình trong hệ thống. Khi cần phát hành hóa đơn tài chính, doanh nghiệp nên đối soát và nhập chi phí chính thức trước khi gửi khách hàng.
                        </div>
                        <div class="total-box">
                          <div class="total-row"><span>Tổng phụ tùng</span><strong>%s</strong></div>
                          <div class="total-row"><span>Phí dịch vụ</span><strong>Chưa cập nhật</strong></div>
                          <div class="total-row grand"><span>Tạm tính</span><strong>%s</strong></div>
                        </div>
                      </section>

                      <section class="footer">
                        <div class="signature">Đại diện khách hàng</div>
                        <div class="signature">Đại diện ServiceOps</div>
                      </section>
                    </section>
                  </main>
                </body>
                </html>
                """.formatted(
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

    private String renderRows(List<InventoryTransaction> consumedParts) {
        if (consumedParts.isEmpty()) {
            return """
                    <tr>
                      <td colspan="5" class="empty">Chưa có phụ tùng được ghi nhận cho phiếu này.</td>
                    </tr>
                    """;
        }

        StringBuilder rows = new StringBuilder();
        int index = 1;
        for (InventoryTransaction tx : consumedParts) {
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
                    escape(tx.getSparePart().getName()),
                    escape(tx.getSparePart().getSku()),
                    formatNumber(tx.getQuantity()),
                    escape(tx.getSparePart().getUnit()),
                    formatMoney(tx.getSparePart().getUnitPrice()),
                    formatMoney(amount(tx))
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

    private BigDecimal amount(InventoryTransaction tx) {
        return tx.getSparePart().getUnitPrice().multiply(tx.getQuantity()).setScale(0, RoundingMode.HALF_UP);
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
