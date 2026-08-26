package com.serviceops.bootstrap;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.asset.domain.AssetStatus;
import com.serviceops.common.domain.Priority;
import com.serviceops.customer.domain.Customer;
import com.serviceops.customer.domain.CustomerRepository;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransaction;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.InventoryTransactionType;
import com.serviceops.inventory.domain.SparePart;
import com.serviceops.inventory.domain.SparePartRepository;
import com.serviceops.scheduling.domain.Appointment;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.scheduling.domain.AppointmentStatus;
import com.serviceops.security.DemoProperties;
import com.serviceops.servicerequest.domain.ServiceChannel;
import com.serviceops.servicerequest.domain.ServiceChannelRepository;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.tenant.domain.Tenant;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoDataFactory {
    private final UserAccountRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final TechnicianRepository technicianRepository;
    private final ServiceChannelRepository serviceChannelRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final AppointmentRepository appointmentRepository;
    private final SparePartRepository sparePartRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoProperties demoProperties;

    void refreshKnownDemoPasswords() {
        String encoded = passwordEncoder.encode(seedPassword());
        List.of("owner", "dispatcher", "customer-service", "technician", "technician-2", "warehouse")
                .forEach(username -> userRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
                    user.setPasswordHash(encoded);
                    userRepository.save(user);
                }));
    }

    UserAccount user(Tenant tenant, String username, String displayName, UserRole role) {
        UserAccount user = new UserAccount();
        user.setTenantId(tenant.getId());
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(seedPassword()));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    String seedPassword() {
        return demoProperties.requireSeedPassword();
    }

    void seedServiceChannels(Tenant tenant) {
        serviceChannel(tenant, "PHONE", "Điện thoại", "Cuộc gọi hotline hoặc số chăm sóc khách hàng", "green", 10);
        serviceChannel(tenant, "EMAIL", "Email", "Yêu cầu gửi qua hộp thư hỗ trợ", "blue", 20);
        serviceChannel(tenant, "WEBSITE", "Website", "Biểu mẫu tiếp nhận trên website hoặc portal", "geekblue", 30);
        serviceChannel(tenant, "ZALO", "Zalo", "Tin nhắn từ Zalo OA hoặc nhân viên CSKH", "cyan", 40);
        serviceChannel(tenant, "WALK_IN", "Trực tiếp", "Khách đến trực tiếp quầy hoặc văn phòng", "orange", 50);
        serviceChannel(tenant, "INTERNAL", "Nội bộ", "Yêu cầu được tạo bởi đội vận hành nội bộ", "purple", 60);
    }

    void serviceChannel(Tenant tenant, String code, String name, String description, String color, int sortOrder) {
        ServiceChannel channel = new ServiceChannel();
        channel.setTenantId(tenant.getId());
        channel.setCode(code);
        channel.setName(name);
        channel.setDescription(description);
        channel.setColor(color);
        channel.setSortOrder(sortOrder);
        channel.setActive(true);
        channel.setSystemDefined(true);
        serviceChannelRepository.save(channel);
    }

    TechnicianProfile technician(Tenant tenant, UserAccount user, String phone, String skills) {
        TechnicianProfile t = new TechnicianProfile();
        t.setTenantId(tenant.getId());
        t.setUser(user);
        t.setPhone(phone);
        t.setSkills(skills);
        t.setActive(true);
        return technicianRepository.save(t);
    }

    Customer customer(Tenant tenant, String code, String name, String phone, String email, String address) {
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setCode(code);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddress(address);
        customer.setActive(true);
        return customerRepository.save(customer);
    }

    Asset asset(Tenant tenant, Customer customer, String category, String brand, String model, String serial, LocalDate warrantyUntil) {
        Asset asset = new Asset();
        asset.setTenantId(tenant.getId());
        asset.setCustomer(customer);
        asset.setCategory(category);
        asset.setBrand(brand);
        asset.setModel(model);
        asset.setSerialNumber(serial);
        asset.setInstalledAt(LocalDate.now().minusMonths(6));
        asset.setWarrantyUntil(warrantyUntil);
        asset.setStatus(AssetStatus.ACTIVE);
        return assetRepository.save(asset);
    }

    ServiceRequest serviceRequest(Tenant tenant, Customer customer, Asset asset, String title, String description,
                                          Priority priority, String channel, String createdBy) {
        ServiceRequest sr = new ServiceRequest();
        sr.setTenantId(tenant.getId());
        sr.setCustomer(customer);
        sr.setAsset(asset);
        sr.setTitle(title);
        sr.setDescription(description);
        sr.setPriority(priority);
        sr.setChannel(channel);
        sr.setStatus(ServiceRequestStatus.OPEN);
        sr.setCreatedBy(createdBy);
        return serviceRequestRepository.save(sr);
    }

    WorkOrder workOrder(Tenant tenant, ServiceRequest sr, Customer customer, Asset asset, TechnicianProfile technician,
                                String summary, Priority priority, WorkOrderStatus status, Instant start, Instant end) {
        WorkOrder wo = new WorkOrder();
        wo.setTenantId(tenant.getId());
        wo.setServiceRequest(sr);
        wo.setCustomer(customer);
        wo.setAsset(asset);
        wo.setTechnician(technician);
        wo.setCode("WO-%d-%06d".formatted(Instant.now().atZone(ZoneOffset.UTC).getYear(), workOrderRepository.nextNumber()));
        wo.setSummary(summary);
        wo.setDescription(sr == null ? summary : sr.getDescription());
        wo.setPriority(priority);
        wo.setStatus(status);
        wo.setScheduledStart(start);
        wo.setScheduledEnd(end);
        WorkOrder saved = workOrderRepository.save(wo);
        if (sr != null) {
            sr.setStatus(ServiceRequestStatus.CONVERTED);
        }
        return saved;
    }

    void appointment(Tenant tenant, WorkOrder workOrder, TechnicianProfile technician) {
        if (workOrder.getScheduledStart() == null || workOrder.getScheduledEnd() == null) {
            return;
        }
        Appointment appointment = new Appointment();
        appointment.setTenantId(tenant.getId());
        appointment.setWorkOrder(workOrder);
        appointment.setTechnician(technician);
        appointment.setStartTime(workOrder.getScheduledStart());
        appointment.setEndTime(workOrder.getScheduledEnd());
        appointment.setStatus(AppointmentStatus.ACTIVE);
        appointmentRepository.save(appointment);
    }

    void history(Tenant tenant, WorkOrder wo, WorkOrderStatus from, WorkOrderStatus to, String note, String changedBy) {
        WorkOrderStatusHistory h = new WorkOrderStatusHistory();
        h.setTenantId(tenant.getId());
        h.setWorkOrder(wo);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setNote(note);
        h.setChangedBy(changedBy);
        historyRepository.save(h);
    }

    SparePart sparePart(Tenant tenant, String sku, String name, String unit, BigDecimal stock, BigDecimal reorder, BigDecimal price) {
        SparePart part = new SparePart();
        part.setTenantId(tenant.getId());
        part.setSku(sku);
        part.setName(name);
        part.setUnit(unit);
        part.setStockQuantity(stock);
        part.setReorderLevel(reorder);
        part.setUnitPrice(price);
        part.setActive(true);
        sparePartRepository.save(part);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setTenantId(tenant.getId());
        tx.setSparePart(part);
        tx.setTransactionType(InventoryTransactionType.IMPORT);
        tx.setQuantity(stock);
        tx.setBalanceAfter(stock);
        tx.setNote("Tồn đầu kỳ demo");
        tx.setCreatedBy("system");
        tx.setActorDisplayName("Hệ thống");
        tx.setActorRole("SYSTEM");
        inventoryTransactionRepository.save(tx);
        return part;
    }
}
