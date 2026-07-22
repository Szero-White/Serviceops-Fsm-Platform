package com.serviceops.bootstrap;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetRepository;
import com.serviceops.asset.domain.AssetStatus;
import com.serviceops.audit.application.AuditService;
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
import com.serviceops.notification.domain.Notification;
import com.serviceops.notification.domain.NotificationRepository;
import com.serviceops.scheduling.domain.Appointment;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.scheduling.domain.AppointmentStatus;
import com.serviceops.servicerequest.domain.RequestChannel;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.servicerequest.domain.ServiceRequestStatus;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.tenant.domain.Tenant;
import com.serviceops.tenant.domain.TenantRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Profile("local")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private final TenantRepository tenantRepository;
    private final UserAccountRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final TechnicianRepository technicianRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final AppointmentRepository appointmentRepository;
    private final SparePartRepository sparePartRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameIgnoreCase("owner")) {
            return;
        }

        Tenant tenant = new Tenant();
        tenant.setCode("DEMO");
        tenant.setName("ServiceOps Demo Company");
        tenant.setActive(true);
        tenantRepository.save(tenant);

        UserAccount owner = user(tenant, "owner", "Nguyễn Minh Quản", UserRole.OWNER);
        UserAccount dispatcher = user(tenant, "dispatcher", "Lê Thu Điều phối", UserRole.DISPATCHER);
        UserAccount customerService = user(tenant, "customer-service", "Trần Mai CSKH", UserRole.CUSTOMER_SERVICE);
        UserAccount technicianUser = user(tenant, "technician", "Phạm Quốc Kỹ thuật", UserRole.TECHNICIAN);
        UserAccount technician2User = user(tenant, "technician-2", "Võ Hoàng Kỹ thuật", UserRole.TECHNICIAN);
        UserAccount warehouse = user(tenant, "warehouse", "Đặng Nam Kho", UserRole.WAREHOUSE_STAFF);

        TechnicianProfile technician = technician(tenant, technicianUser, "0909123456", "Máy lạnh, điện dân dụng, vệ sinh định kỳ");
        TechnicianProfile technician2 = technician(tenant, technician2User, "0909765432", "Tủ lạnh, máy giặt, điện lạnh dân dụng");

        List<Customer> customers = new ArrayList<>();
        customers.add(customer(tenant, "KH-0001", "Công ty TNHH An Phát", "0903001001", "contact@anphat.vn", "12 Nguyễn Văn Trỗi, Phú Nhuận"));
        customers.add(customer(tenant, "KH-0002", "Cửa hàng Minh Anh", "0903001002", "minhanh@example.com", "45 Cộng Hòa, Tân Bình"));
        customers.add(customer(tenant, "KH-0003", "Nguyễn Hoàng Nam", "0903001003", "nam.nguyen@example.com", "88 Lê Văn Sỹ, Quận 3"));
        customers.add(customer(tenant, "KH-0004", "Văn phòng Sao Việt", "0903001004", "admin@saoviet.vn", "102 Điện Biên Phủ, Bình Thạnh"));
        customers.add(customer(tenant, "KH-0005", "Nhà hàng Bếp Xanh", "0903001005", "bepxanh@example.com", "21 Nguyễn Thị Minh Khai, Quận 1"));

        Asset asset1 = asset(tenant, customers.get(0), "Máy lạnh", "Daikin", "FTKC35", "DK-FTKC35-0001", LocalDate.now().plusYears(1));
        Asset asset2 = asset(tenant, customers.get(1), "Máy lạnh", "Panasonic", "CU-PU12", "PN-CUPU12-0002", LocalDate.now().plusMonths(8));
        Asset asset3 = asset(tenant, customers.get(2), "Tủ lạnh", "Samsung", "RT38", "SS-RT38-0003", LocalDate.now().minusMonths(2));
        Asset asset4 = asset(tenant, customers.get(3), "Máy lạnh", "Mitsubishi", "MSY-GR35", "MT-MSYGR35-0004", LocalDate.now().plusYears(2));
        Asset asset5 = asset(tenant, customers.get(4), "Tủ đông", "Sanaky", "VH-8699HY", "SK-VH8699-0005", LocalDate.now().plusMonths(4));

        ServiceRequest sr1 = serviceRequest(tenant, customers.get(0), asset1, "Máy lạnh không đủ lạnh", "Máy chạy nhưng nhiệt độ phòng không giảm, có tiếng ồn nhẹ.", Priority.HIGH, RequestChannel.PHONE, customerService.getUsername());
        ServiceRequest sr2 = serviceRequest(tenant, customers.get(1), asset2, "Bảo trì định kỳ 6 tháng", "Vệ sinh dàn nóng, dàn lạnh và kiểm tra gas.", Priority.NORMAL, RequestChannel.ZALO, customerService.getUsername());
        ServiceRequest sr3 = serviceRequest(tenant, customers.get(2), asset3, "Tủ lạnh đóng tuyết", "Ngăn đông đóng tuyết dày, ngăn mát yếu.", Priority.URGENT, RequestChannel.WEBSITE, customerService.getUsername());
        serviceRequest(tenant, customers.get(4), asset5, "Tủ đông phát tiếng kêu", "Tiếng kêu lớn khi máy nén khởi động.", Priority.NORMAL, RequestChannel.PHONE, customerService.getUsername());

        WorkOrder wo1 = workOrder(tenant, sr1, customers.get(0), asset1, technician, "Kiểm tra hệ thống lạnh", Priority.HIGH,
                WorkOrderStatus.ASSIGNED, Instant.now().plus(2, ChronoUnit.HOURS), Instant.now().plus(4, ChronoUnit.HOURS));
        WorkOrder wo2 = workOrder(tenant, sr2, customers.get(1), asset2, technician2, "Bảo trì máy lạnh định kỳ", Priority.NORMAL,
                WorkOrderStatus.IN_PROGRESS, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS));
        WorkOrder wo3 = workOrder(tenant, sr3, customers.get(2), asset3, null, "Khắc phục tủ lạnh đóng tuyết", Priority.URGENT,
                WorkOrderStatus.OPEN, null, null);
        WorkOrder wo4 = workOrder(tenant, null, customers.get(3), asset4, technician, "Vệ sinh hệ thống điều hòa văn phòng", Priority.NORMAL,
                WorkOrderStatus.COMPLETED, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(2, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS));
        wo4.setDiagnosis("Dàn lạnh bám bụi, lưu lượng gió giảm.");
        wo4.setResolution("Vệ sinh dàn lạnh, kiểm tra dòng và áp suất gas.");
        wo4.setCompletedAt(Instant.now().minus(2, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS));

        appointment(tenant, wo1, technician);
        appointment(tenant, wo2, technician2);
        appointment(tenant, wo4, technician);

        history(tenant, wo1, null, WorkOrderStatus.OPEN, "Tạo từ yêu cầu dịch vụ", dispatcher.getUsername());
        history(tenant, wo1, WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, "Phân công kỹ thuật viên", dispatcher.getUsername());
        history(tenant, wo2, null, WorkOrderStatus.OPEN, "Tạo từ yêu cầu dịch vụ", dispatcher.getUsername());
        history(tenant, wo2, WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, "Phân công kỹ thuật viên", dispatcher.getUsername());
        history(tenant, wo2, WorkOrderStatus.ASSIGNED, WorkOrderStatus.ON_THE_WAY, "Đang di chuyển", technician2User.getUsername());
        history(tenant, wo2, WorkOrderStatus.ON_THE_WAY, WorkOrderStatus.IN_PROGRESS, "Bắt đầu công việc", technician2User.getUsername());
        history(tenant, wo3, null, WorkOrderStatus.OPEN, "Tạo từ yêu cầu dịch vụ", dispatcher.getUsername());
        history(tenant, wo4, null, WorkOrderStatus.OPEN, "Tạo trực tiếp", dispatcher.getUsername());
        history(tenant, wo4, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.COMPLETED, "Hoàn tất công việc", technicianUser.getUsername());

        sparePart(tenant, "GAS-R32-1KG", "Gas lạnh R32", "kg", new BigDecimal("12.500"), new BigDecimal("3.000"), new BigDecimal("285000"));
        sparePart(tenant, "CAP-35UF", "Tụ điện 35µF", "cái", new BigDecimal("8"), new BigDecimal("3"), new BigDecimal("145000"));
        sparePart(tenant, "FILTER-AC-01", "Lưới lọc máy lạnh tiêu chuẩn", "cái", new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("95000"));
        sparePart(tenant, "SENSOR-TEMP-10K", "Cảm biến nhiệt độ 10K", "cái", new BigDecimal("15"), new BigDecimal("5"), new BigDecimal("120000"));
        sparePart(tenant, "COPPER-6-10", "Ống đồng 6/10", "m", new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("85000"));

        Notification notification = new Notification();
        notification.setTenantId(tenant.getId());
        notification.setRecipient(technicianUser);
        notification.setTitle("Công việc mới: " + wo1.getCode());
        notification.setMessage("Bạn được phân công kiểm tra máy lạnh tại Công ty TNHH An Phát.");
        notificationRepository.save(notification);

        auditService.recordAs(tenant.getId(), "system", "SEED", "SYSTEM", tenant.getId(), "Khởi tạo dữ liệu demo local-first");
    }

    private UserAccount user(Tenant tenant, String username, String displayName, UserRole role) {
        UserAccount user = new UserAccount();
        user.setTenantId(tenant.getId());
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    private TechnicianProfile technician(Tenant tenant, UserAccount user, String phone, String skills) {
        TechnicianProfile t = new TechnicianProfile();
        t.setTenantId(tenant.getId());
        t.setUser(user);
        t.setPhone(phone);
        t.setSkills(skills);
        t.setActive(true);
        return technicianRepository.save(t);
    }

    private Customer customer(Tenant tenant, String code, String name, String phone, String email, String address) {
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

    private Asset asset(Tenant tenant, Customer customer, String category, String brand, String model, String serial, LocalDate warrantyUntil) {
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

    private ServiceRequest serviceRequest(Tenant tenant, Customer customer, Asset asset, String title, String description,
                                          Priority priority, RequestChannel channel, String createdBy) {
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

    private WorkOrder workOrder(Tenant tenant, ServiceRequest sr, Customer customer, Asset asset, TechnicianProfile technician,
                                String summary, Priority priority, WorkOrderStatus status, Instant start, Instant end) {
        WorkOrder wo = new WorkOrder();
        wo.setTenantId(tenant.getId());
        wo.setServiceRequest(sr);
        wo.setCustomer(customer);
        wo.setAsset(asset);
        wo.setTechnician(technician);
        wo.setCode("WO-2026-%06d".formatted(workOrderRepository.nextNumber()));
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

    private void appointment(Tenant tenant, WorkOrder workOrder, TechnicianProfile technician) {
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

    private void history(Tenant tenant, WorkOrder wo, WorkOrderStatus from, WorkOrderStatus to, String note, String changedBy) {
        WorkOrderStatusHistory h = new WorkOrderStatusHistory();
        h.setTenantId(tenant.getId());
        h.setWorkOrder(wo);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setNote(note);
        h.setChangedBy(changedBy);
        historyRepository.save(h);
    }

    private SparePart sparePart(Tenant tenant, String sku, String name, String unit, BigDecimal stock, BigDecimal reorder, BigDecimal price) {
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
        inventoryTransactionRepository.save(tx);
        return part;
    }
}
