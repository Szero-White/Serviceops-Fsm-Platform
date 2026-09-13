package com.serviceops.common.exception;

import com.serviceops.common.web.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("rawText", "Nội dung mô tả"),
            Map.entry("question", "Câu hỏi"),
            Map.entry("currentPath", "Màn hình hiện tại"),
            Map.entry("customerId", "Khách hàng"),
            Map.entry("category", "Loại thiết bị"),
            Map.entry("brand", "Hãng"),
            Map.entry("model", "Dòng / mẫu"),
            Map.entry("serialNumber", "Số sê-ri"),
            Map.entry("notes", "Ghi chú"),
            Map.entry("method", "Phương thức thanh toán"),
            Map.entry("bankName", "Ngân hàng"),
            Map.entry("accountHolder", "Chủ tài khoản"),
            Map.entry("accountNumber", "Số tài khoản"),
            Map.entry("phone", "Số điện thoại"),
            Map.entry("skills", "Kỹ năng"),
            Map.entry("code", "Mã"),
            Map.entry("name", "Tên"),
            Map.entry("email", "Email"),
            Map.entry("address", "Địa chỉ"),
            Map.entry("title", "Tiêu đề"),
            Map.entry("description", "Mô tả"),
            Map.entry("priority", "Mức độ ưu tiên"),
            Map.entry("channel", "Kênh tiếp nhận"),
            Map.entry("sparePartId", "Phụ tùng"),
            Map.entry("quantity", "Số lượng"),
            Map.entry("note", "Ghi chú"),
            Map.entry("reason", "Lý do"),
            Map.entry("usedQuantity", "Số lượng đã sử dụng"),
            Map.entry("sku", "Mã phụ tùng"),
            Map.entry("unit", "Đơn vị"),
            Map.entry("initialStock", "Tồn ban đầu"),
            Map.entry("reorderLevel", "Ngưỡng tồn tối thiểu"),
            Map.entry("unitPrice", "Đơn giá"),
            Map.entry("active", "Trạng thái hoạt động"),
            Map.entry("actualQuantity", "Tồn thực tế"),
            Map.entry("color", "Màu hiển thị"),
            Map.entry("sortOrder", "Thứ tự hiển thị"),
            Map.entry("laborFee", "Chi phí công"),
            Map.entry("incidentalFee", "Chi phí phát sinh"),
            Map.entry("incidentalReason", "Lý do phát sinh"),
            Map.entry("reviewedTotalAmount", "Tổng tiền xác nhận"),
            Map.entry("reviewToken", "Thông tin xác nhận"),
            Map.entry("technicianId", "Kỹ thuật viên"),
            Map.entry("startTime", "Thời gian bắt đầu"),
            Map.entry("endTime", "Thời gian kết thúc"),
            Map.entry("targetStatus", "Trạng thái"),
            Map.entry("diagnosis", "Chẩn đoán"),
            Map.entry("resolution", "Giải pháp"),
            Map.entry("username", "Tên đăng nhập"),
            Map.entry("displayName", "Họ tên"),
            Map.entry("role", "Vai trò"),
            Map.entry("password", "Mật khẩu")
    );

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setTitle(ex.getCode());
        detail.setType(URI.create("https://serviceops.local/problems/" + ex.getCode().toLowerCase()));
        detail.setProperty("code", ex.getCode());
        return addRequestMetadata(detail, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
        detail.setTitle("VALIDATION_ERROR");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), validationMessage(error.getField(), error.getCode()))
        );
        detail.setProperty("code", "VALIDATION_ERROR");
        detail.setProperty("errors", errors);
        return addRequestMetadata(detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Nội dung gửi lên không hợp lệ. Vui lòng kiểm tra các trường và thử lại."
        );
        detail.setTitle("REQUEST_BODY_INVALID");
        detail.setType(URI.create("https://serviceops.local/problems/request_body_invalid"));
        detail.setProperty("code", "REQUEST_BODY_INVALID");
        return addRequestMetadata(detail, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConstraint(DataIntegrityViolationException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Dữ liệu bị trùng hoặc không phù hợp với dữ liệu hiện có."
        );
        detail.setTitle("DATA_INTEGRITY_VIOLATION");
        detail.setProperty("code", "DATA_INTEGRITY_VIOLATION");
        return addRequestMetadata(detail, request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Dữ liệu vừa được thay đổi bởi thao tác khác. Vui lòng tải lại và thử lại."
        );
        detail.setTitle("CONCURRENT_MODIFICATION");
        detail.setProperty("code", "CONCURRENT_MODIFICATION");
        return addRequestMetadata(detail, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Thao tác này không được hỗ trợ tại chức năng hiện tại."
        );
        detail.setTitle("METHOD_NOT_ALLOWED");
        detail.setType(URI.create("https://serviceops.local/problems/method_not_allowed"));
        detail.setProperty("code", "METHOD_NOT_ALLOWED");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(ex.getHeaders())
                .body(addRequestMetadata(detail, request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy dữ liệu hoặc chức năng được yêu cầu."
        );
        detail.setTitle("RESOURCE_NOT_FOUND");
        detail.setType(URI.create("https://serviceops.local/problems/resource_not_found"));
        detail.setProperty("code", "RESOURCE_NOT_FOUND");
        return addRequestMetadata(detail, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
        detail.setTitle("ACCESS_DENIED");
        detail.setProperty("code", "ACCESS_DENIED");
        return addRequestMetadata(detail, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled request failure: {} {}", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi ngoài dự kiến");
        detail.setTitle("INTERNAL_SERVER_ERROR");
        detail.setProperty("code", "INTERNAL_SERVER_ERROR");
        return addRequestMetadata(detail, request);
    }

    private static String validationMessage(String field, String validationCode) {
        String label = FIELD_LABELS.getOrDefault(field, "Trường này");
        return switch (validationCode == null ? "" : validationCode) {
            case "NotBlank", "NotEmpty", "NotNull" -> label + " là bắt buộc";
            case "Email" -> "Email không hợp lệ";
            case "Pattern" -> label + " không đúng định dạng";
            case "Size", "Length" -> label + " không đúng độ dài cho phép";
            case "Future", "FutureOrPresent" -> label + " phải ở thời điểm hiện tại hoặc tương lai";
            case "Positive", "PositiveOrZero", "DecimalMin", "Min" -> label + " phải lớn hơn hoặc bằng giá trị tối thiểu";
            case "Negative", "NegativeOrZero", "DecimalMax", "Max" -> label + " vượt quá giá trị cho phép";
            default -> label + " không hợp lệ";
        };
    }

    private static ProblemDetail addRequestMetadata(ProblemDetail detail, HttpServletRequest request) {
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        Object requestId = request.getAttribute(RequestCorrelationFilter.MDC_KEY);
        if (requestId != null) {
            detail.setProperty("requestId", requestId);
        }
        return detail;
    }
}
