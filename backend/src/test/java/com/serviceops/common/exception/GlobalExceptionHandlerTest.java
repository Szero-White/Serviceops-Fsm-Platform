package com.serviceops.common.exception;

import com.serviceops.common.web.RequestCorrelationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void optimisticLockFailureIsReportedAsConflictWithRequestId() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/work-orders/123");
        request.setAttribute(RequestCorrelationFilter.MDC_KEY, "req-123");

        var detail = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException("WorkOrder", UUID.randomUUID()),
                request
        );

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(detail.getTitle()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(detail.getProperties()).containsEntry("code", "CONCURRENT_MODIFICATION");
        assertThat(detail.getProperties()).containsEntry("requestId", "req-123");
    }

    @Test
    void unsupportedHttpMethodIsReportedAsMethodNotAllowed() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/work-orders");
        request.setAttribute(RequestCorrelationFilter.MDC_KEY, "req-405");

        var response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("POST", List.of("GET")),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getAllow()).contains(HttpMethod.GET);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(response.getBody().getProperties()).containsEntry("code", "METHOD_NOT_ALLOWED");
        assertThat(response.getBody().getProperties()).containsEntry("requestId", "req-405");
    }
}
