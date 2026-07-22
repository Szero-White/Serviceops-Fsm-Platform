package com.serviceops.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setTitle(ex.getCode());
        detail.setType(URI.create("https://serviceops.local/problems/" + ex.getCode().toLowerCase()));
        detail.setProperty("code", ex.getCode());
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
        detail.setTitle("VALIDATION_ERROR");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("code", "VALIDATION_ERROR");
        detail.setProperty("errors", errors);
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        return detail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConstraint(DataIntegrityViolationException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc vi phạm ràng buộc hệ thống");
        detail.setTitle("DATA_INTEGRITY_VIOLATION");
        detail.setProperty("code", "DATA_INTEGRITY_VIOLATION");
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        return detail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
        detail.setTitle("ACCESS_DENIED");
        detail.setProperty("code", "ACCESS_DENIED");
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi ngoài dự kiến");
        detail.setTitle("INTERNAL_SERVER_ERROR");
        detail.setProperty("code", "INTERNAL_SERVER_ERROR");
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("path", request.getRequestURI());
        return detail;
    }
}
