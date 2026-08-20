package com.serviceops.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DemoProtectionFilterTest {

    @Test
    void blocksDeleteInDemoMode() throws Exception {
        var fixture = fixture(true, "DELETE", "/api/v1/customers/123");

        fixture.filter().doFilter(fixture.request(), fixture.response(), fixture.chain());

        assertThat(fixture.response().getStatus()).isEqualTo(403);
        assertThat(fixture.response().getContentAsString())
                .contains("DEMO_WRITE_PROTECTED");
        verifyNoInteractions(fixture.chain());
    }

    @Test
    void keepsUserAdministrationWritesAvailableInDemoMode() throws Exception {
        for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
            var fixture = fixture(true, method, "/api/v1/users/123");

            fixture.filter().doFilter(
                    fixture.request(),
                    fixture.response(),
                    fixture.chain()
            );

            verify(fixture.chain()).doFilter(
                    fixture.request(),
                    fixture.response()
            );
        }
    }

    @Test
    void keepsTechnicianProfileWritesAvailableInDemoMode() throws Exception {
        var fixture = fixture(true, "PUT", "/api/v1/technicians/123");

        fixture.filter().doFilter(
                fixture.request(),
                fixture.response(),
                fixture.chain()
        );

        verify(fixture.chain()).doFilter(
                fixture.request(),
                fixture.response()
        );
    }

    @Test
    void blocksServiceChannelAdministrationWritesInDemoMode() throws Exception {
        var fixture = fixture(true, "PUT", "/api/v1/service-channels/123");

        fixture.filter().doFilter(
                fixture.request(),
                fixture.response(),
                fixture.chain()
        );

        assertThat(fixture.response().getStatus()).isEqualTo(403);
        verifyNoInteractions(fixture.chain());
    }

    @Test
    void keepsAdministrationReadsAvailableInDemoMode() throws Exception {
        var fixture = fixture(true, "GET", "/api/v1/users");

        fixture.filter().doFilter(
                fixture.request(),
                fixture.response(),
                fixture.chain()
        );

        verify(fixture.chain()).doFilter(
                fixture.request(),
                fixture.response()
        );
    }

    @Test
    void keepsCoreWorkflowWritesAvailableInDemoMode() throws Exception {
        for (String path : new String[]{
                "/api/v1/customers",
                "/api/v1/service-requests",
                "/api/v1/work-orders",
                "/api/v1/spare-parts"
        }) {
            var fixture = fixture(true, "POST", path);

            fixture.filter().doFilter(
                    fixture.request(),
                    fixture.response(),
                    fixture.chain()
            );

            verify(fixture.chain()).doFilter(
                    fixture.request(),
                    fixture.response()
            );
        }
    }

    @Test
    void doesNotOvermatchSimilarUnprotectedPaths() throws Exception {
        var fixture = fixture(true, "POST", "/api/v1/users-report");

        fixture.filter().doFilter(
                fixture.request(),
                fixture.response(),
                fixture.chain()
        );

        verify(fixture.chain()).doFilter(
                fixture.request(),
                fixture.response()
        );
    }

    @Test
    void doesNotChangeBehaviorWhenDemoModeIsDisabled() throws Exception {
        var fixture = fixture(false, "DELETE", "/api/v1/customers/123");

        fixture.filter().doFilter(
                fixture.request(),
                fixture.response(),
                fixture.chain()
        );

        verify(fixture.chain()).doFilter(
                fixture.request(),
                fixture.response()
        );
    }

    private static Fixture fixture(boolean enabled, String method, String path) {
        DemoProtectionFilter filter = new DemoProtectionFilter(
                new DemoProperties(enabled, "demo-password"),
                new ObjectMapper().findAndRegisterModules()
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(method, path);
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        return new Fixture(filter, request, response, chain);
    }

    private record Fixture(
            DemoProtectionFilter filter,
            MockHttpServletRequest request,
            MockHttpServletResponse response,
            FilterChain chain
    ) {
    }
}
