package com.serviceops.common.exception;

import com.serviceops.common.web.RequestCorrelationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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
}
