package com.serviceops.asset.application;

import com.serviceops.asset.application.AssetCsvService.AssetCsvRow;
import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.asset.domain.AssetStatus;
import com.serviceops.asset.web.AssetDtos.AssetRequest;
import com.serviceops.attachment.domain.AttachmentRepository;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetInactiveCustomerPolicyTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private AssetRepository repository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private AssetCsvService csvService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private AssetService service;

    @BeforeEach
    void setUp() {
        service = new AssetService(
                repository,
                customerRepository,
                serviceRequestRepository,
                workOrderRepository,
                attachmentRepository,
                csvService,
                auditService,
                notificationService
        );
        authenticate();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactiveCustomerCannotReceiveNewAsset() {
        Customer customer = customer(false);
        when(customerRepository.findByIdAndTenantId(customer.getId(), TENANT_ID)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.create(request(customer.getId(), "SERIAL-NEW")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("CUSTOMER_INACTIVE");
                    assertThat(ex.getMessage()).contains("ngừng hoạt động");
                });
    }

    @Test
    void existingAssetCanStillBeEditedUnderSameInactiveCustomer() {
        Customer customer = customer(false);
        Asset asset = existingAsset(customer);

        when(repository.findDetailed(asset.getId(), TENANT_ID)).thenReturn(Optional.of(asset));
        when(customerRepository.findByIdAndTenantId(customer.getId(), TENANT_ID)).thenReturn(Optional.of(customer));

        var response = service.update(asset.getId(), request(customer.getId(), asset.getSerialNumber()));

        assertThat(response.customerId()).isEqualTo(customer.getId());
        assertThat(response.category()).isEqualTo("Máy lạnh");
    }

    @Test
    void existingAssetCannotBeReassignedToDifferentInactiveCustomer() {
        Customer current = customer(true);
        Customer inactive = customer(false);
        Asset asset = existingAsset(current);

        when(repository.findDetailed(asset.getId(), TENANT_ID)).thenReturn(Optional.of(asset));
        when(customerRepository.findByIdAndTenantId(inactive.getId(), TENANT_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.update(asset.getId(), request(inactive.getId(), asset.getSerialNumber())))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("CUSTOMER_INACTIVE"));
    }

    @Test
    void assetImportRejectsInactiveCustomer() {
        Customer inactive = customer(false);
        MultipartFile file = mock(MultipartFile.class);
        AssetCsvRow row = new AssetCsvRow(
                2,
                inactive.getCode(),
                "Máy lạnh",
                "Daikin",
                "FTKC35",
                "SERIAL-IMPORT",
                "",
                "",
                "ACTIVE",
                ""
        );

        when(csvService.parseAssets(file)).thenReturn(List.of(row));
        when(customerRepository.findByTenantIdAndCodeIgnoreCase(TENANT_ID, inactive.getCode().toUpperCase(java.util.Locale.ROOT))).thenReturn(Optional.of(inactive));

        var result = service.importAssets(file, false);

        assertThat(result.validRows()).isZero();
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.rows()).singleElement().satisfies(item -> {
            assertThat(item.valid()).isFalse();
            assertThat(item.message()).contains("ngừng hoạt động");
        });
    }

    private static AssetRequest request(UUID customerId, String serial) {
        return new AssetRequest(
                customerId,
                "Máy lạnh",
                "Daikin",
                "FTKC35",
                serial,
                null,
                null,
                AssetStatus.ACTIVE,
                null
        );
    }

    private static Customer customer(boolean active) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setCode(active ? "CUS-ACTIVE-" + customer.getId().toString().substring(0, 6) : "CUS-INACTIVE-" + customer.getId().toString().substring(0, 6));
        customer.setName(active ? "Khách đang hoạt động" : "Khách đã ngừng hoạt động");
        customer.setActive(active);
        return customer;
    }

    private static Asset existingAsset(Customer customer) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setTenantId(TENANT_ID);
        asset.setCustomer(customer);
        asset.setCategory("Máy lạnh cũ");
        asset.setBrand("Daikin");
        asset.setModel("FTKC35");
        asset.setSerialNumber("SERIAL-EXISTING");
        asset.setStatus(AssetStatus.ACTIVE);
        asset.setCreatedAt(Instant.now());
        return asset;
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
