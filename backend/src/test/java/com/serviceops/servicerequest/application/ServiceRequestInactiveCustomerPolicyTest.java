package com.serviceops.servicerequest.application;

import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.common.domain.Priority;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.servicerequest.domain.ServiceChannel;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.servicerequest.web.ServiceRequestDtos.CreateServiceRequest;
import com.serviceops.workorder.domain.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestInactiveCustomerPolicyTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private ServiceRequestRepository repository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private ServiceChannelService serviceChannelService;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private AuditService auditService;

    private ServiceRequestService service;

    @BeforeEach
    void setUp() {
        service = new ServiceRequestService(
                repository,
                customerRepository,
                assetRepository,
                serviceChannelService,
                workOrderRepository,
                attachmentRepository,
                auditService
        );
        authenticate();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactiveCustomerCannotStartNewServiceRequest() {
        Customer customer = customer(false);
        when(customerRepository.findByIdAndTenantId(customer.getId(), TENANT_ID)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.create(request(customer.getId())))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("CUSTOMER_INACTIVE");
                    assertThat(ex.getMessage()).contains("ngừng hoạt động");
                });
    }

    @Test
    void existingOpenRequestCanStillBeEditedAfterCustomerDeactivation() {
        Customer customer = customer(false);
        ServiceRequest existing = existingRequest(customer);
        ServiceChannel channel = activeChannel();

        when(repository.findDetailed(existing.getId(), TENANT_ID)).thenReturn(Optional.of(existing));
        when(customerRepository.findByIdAndTenantId(customer.getId(), TENANT_ID)).thenReturn(Optional.of(customer));
        when(serviceChannelService.requireActive(TENANT_ID, "PHONE")).thenReturn(channel);

        var response = service.update(existing.getId(), request(customer.getId()));

        assertThat(response.customerId()).isEqualTo(customer.getId());
        assertThat(response.title()).isEqualTo("Máy lạnh không lạnh");
    }

    @Test
    void existingRequestCannotBeReassignedToDifferentInactiveCustomer() {
        Customer current = customer(true);
        Customer inactive = customer(false);
        ServiceRequest existing = existingRequest(current);

        when(repository.findDetailed(existing.getId(), TENANT_ID)).thenReturn(Optional.of(existing));
        when(customerRepository.findByIdAndTenantId(inactive.getId(), TENANT_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.update(existing.getId(), request(inactive.getId())))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("CUSTOMER_INACTIVE"));
    }

    private static CreateServiceRequest request(UUID customerId) {
        return new CreateServiceRequest(
                customerId,
                null,
                "Máy lạnh không lạnh",
                "Khách báo máy lạnh chạy nhưng không đủ lạnh",
                Priority.NORMAL,
                "PHONE"
        );
    }

    private static Customer customer(boolean active) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setCode(active ? "CUS-ACTIVE" : "CUS-INACTIVE");
        customer.setName(active ? "Khách đang hoạt động" : "Khách đã ngừng hoạt động");
        customer.setActive(active);
        return customer;
    }

    private static ServiceRequest existingRequest(Customer customer) {
        ServiceRequest request = new ServiceRequest();
        request.setId(UUID.randomUUID());
        request.setTenantId(TENANT_ID);
        request.setCustomer(customer);
        request.setTitle("Nội dung cũ");
        request.setDescription("Mô tả cũ");
        request.setPriority(Priority.NORMAL);
        request.setChannel("PHONE");
        request.setStatus(ServiceRequestStatus.OPEN);
        request.setCreatedBy("customer-service");
        request.setCreatedAt(Instant.now());
        return request;
    }

    private static ServiceChannel activeChannel() {
        ServiceChannel channel = new ServiceChannel();
        channel.setId(UUID.randomUUID());
        channel.setTenantId(TENANT_ID);
        channel.setCode("PHONE");
        channel.setName("Điện thoại");
        channel.setActive(true);
        return channel;
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
