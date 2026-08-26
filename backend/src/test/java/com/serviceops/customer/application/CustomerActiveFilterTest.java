package com.serviceops.customer.application;

import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerActiveFilterTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private CustomerRepository repository;
    @Mock private AssetRepository assetRepository;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private CustomerCsvService csvService;
    @Mock private AuditService auditService;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(
                repository,
                assetRepository,
                serviceRequestRepository,
                workOrderRepository,
                csvService,
                auditService
        );
        authenticate();
        when(repository.search(eq(TENANT_ID), nullable(Boolean.class), eq(""), any(Pageable.class)))
                .thenReturn(Page.<Customer>empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchForNewWorkflowCanRequestOnlyActiveCustomers() {
        service.search("", true, 0, 100);

        verify(repository).search(eq(TENANT_ID), eq(Boolean.TRUE), eq(""), any(Pageable.class));
    }

    @Test
    void customerManagementCanRequestOnlyInactiveCustomers() {
        service.search("", false, 0, 20);

        verify(repository).search(eq(TENANT_ID), eq(Boolean.FALSE), eq(""), any(Pageable.class));
    }

    @Test
    void customerManagementCanRequestAllStatuses() {
        service.search("", null, 0, 20);

        verify(repository).search(eq(TENANT_ID), isNull(), eq(""), any(Pageable.class));
    }

    private static void authenticate() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("customer-service")
                .claim("tenantId", TENANT_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("displayName", "CSKH")
                .claim("roles", List.of(UserRole.CUSTOMER_SERVICE.name()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
