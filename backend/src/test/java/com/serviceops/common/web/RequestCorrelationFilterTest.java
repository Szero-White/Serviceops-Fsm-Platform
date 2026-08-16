package com.serviceops.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestCorrelationFilterTest {

    @Test
    void preservesSafeIncomingRequestIdAndReturnsItToCaller() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
        request.addHeader(RequestCorrelationFilter.HEADER, "edge-req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER)).isEqualTo("edge-req-123");
        assertThat(request.getAttribute(RequestCorrelationFilter.MDC_KEY)).isEqualTo("edge-req-123");
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void replacesUnsafeRequestIdInsteadOfPuttingLogControlCharactersIntoMdc() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(RequestCorrelationFilter.HEADER, "bad\nrequest-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER))
                .matches("[0-9a-f\\-]{36}")
                .doesNotContain("\n", "\r");
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }
}
