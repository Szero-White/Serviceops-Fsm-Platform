package com.serviceops.bootstrap;

import com.serviceops.asset.domain.Asset;
import com.serviceops.audit.application.AuditService;
import com.serviceops.common.domain.Priority;
import com.serviceops.customer.domain.Customer;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserAccountRepository;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.domain.Notification;
import com.serviceops.notification.domain.NotificationRepository;
import com.serviceops.security.DemoProperties;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.tenant.domain.Tenant;
import com.serviceops.tenant.domain.TenantRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"local", "demo"})
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private final TenantRepository tenantRepository;
    private final UserAccountRepository userRepository;
    private final DemoDataFactory demoDataFactory;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;
    private final DemoProperties demoProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameIgnoreCase("owner")) {
            if (demoProperties.enabled()) {
                demoDataFactory.refreshKnownDemoPasswords();
            }
            return;
        }

        Tenant tenant = new Tenant();
        tenant.setCode("DEMO");
        tenant.setName("ServiceOps Demo Company");
        tenant.setActive(true);
        tenantRepository.save(tenant);
        demoDataFactory.seedServiceChannels(tenant);

        demoDataFactory.user(tenant, "owner", "Nguyễn Minh Quản", UserRole.OWNER);
        UserAccount dispatcher = demoDataFactory.user(tenant, "dispatcher", "Lê Thu Điều phối", UserRole.DISPATCHER);
        UserAccount customerService = demoDataFactory.user(tenant, "customer-service", "Trần Mai CSKH", UserRole.CUSTOMER_SERVICE);
        UserAccount technicianUser = demoDataFactory.user(tenant, "technician", "Phạm Quốc Kỹ thuật", UserRole.TECHNICIAN);
        UserAccount technician2User = demoDataFactory.user(tenant, "technician-2", "Võ Hoàng Kỹ thuật", UserRole.TECHNICIAN);
        demoDataFactory.user(tenant, "warehouse", "Đặng Nam Kho", UserRole.WAREHOUSE_STAFF);

        TechnicianProfile technician = demoDataFactory.technician(tenant, technicianUser, "0909123456", "Máy lạnh, điện dân dụng, vệ sinh định kỳ");
        TechnicianProfile technician2 = demoDataFactory.technician(tenant, technician2User, "0909765432", "Tủ lạnh, máy giặt, điện lạnh dân dụng");

        List<Customer> customers = new ArrayList<>();
        customers.add(demoDataFactory.customer(tenant, "KH-0001", "Công ty TNHH An Phát", "0903001001", "contact@anphat.vn", "12 Nguyễn Văn Trỗi, Phú Nhuận"));
        customers.add(demoDataFactory.customer(tenant, "KH-0002", "Cửa hàng Minh Anh", "0903001002", "minhanh@example.com", "45 Cộng Hòa, Tân Bình"));
        customers.add(demoDataFactory.customer(tenant, "KH-0003", "Nguyễn Hoàng Nam", "0903001003", "nam.nguyen@example.com", "88 Lê Văn Sỹ, Quận 3"));
        customers.add(demoDataFactory.customer(tenant, "KH-0004", "Văn phòng Sao Việt", "0903001004", "admin@saoviet.vn", "102 Điện Biên Phủ, Bình Thạnh"));
        customers.add(demoDataFactory.customer(tenant, "KH-0005", "Nhà hàng Bếp Xanh", "0903001005", "bepxanh@example.com", "21 Nguyễn Thị Minh Khai, Quận 1"));

        Asset asset1 = demoDataFactory.asset(tenant, customers.get(0), "Máy lạnh", "Daikin", "FTKC35", "DK-FTKC35-0001", LocalDate.now().plusYears(1));
        Asset asset2 = demoDataFactory.asset(tenant, customers.get(1), "Máy lạnh", "Panasonic", "CU-PU12", "PN-CUPU12-0002", LocalDate.now().plusMonths(8));
        Asset asset3 = demoDataFactory.asset(tenant, customers.get(2), "Tủ lạnh", "Samsung", "RT38", "SS-RT38-0003", LocalDate.now().minusMonths(2));
        Asset asset4 = demoDataFactory.asset(tenant, customers.get(3), "Máy lạnh", "Mitsubishi", "MSY-GR35", "MT-MSYGR35-0004", LocalDate.now().plusYears(2));
        Asset asset5 = demoDataFactory.asset(tenant, customers.get(4), "Tủ đông", "Sanaky", "VH-8699HY", "SK-VH8699-0005", LocalDate.now().plusMonths(4));

        ServiceRequest sr1 = demoDataFactory.serviceRequest(tenant, customers.get(0), asset1, "Máy lạnh không đủ lạnh", "Máy chạy nhưng nhiệt độ phòng không giảm, có tiếng ồn nhẹ.", Priority.HIGH, "PHONE", customerService.getUsername());
        ServiceRequest sr2 = demoDataFactory.serviceRequest(tenant, customers.get(1), asset2, "Bảo trì định kỳ 6 tháng", "Vệ sinh dàn nóng, dàn lạnh và kiểm tra gas.", Priority.NORMAL, "ZALO", customerService.getUsername());
        ServiceRequest sr3 = demoDataFactory.serviceRequest(tenant, customers.get(2), asset3, "Tủ lạnh đóng tuyết", "Ngăn đông đóng tuyết dày, ngăn mát yếu.", Priority.URGENT, "WEBSITE", customerService.getUsername());
        ServiceRequest sr4 = demoDataFactory.serviceRequest(tenant, customers.get(3), asset4, "Vệ sinh hệ thống điều hòa văn phòng", "Khách hàng yêu cầu vệ sinh hệ thống điều hòa văn phòng.", Priority.NORMAL, "PHONE", customerService.getUsername());
        demoDataFactory.serviceRequest(tenant, customers.get(4), asset5, "Tủ đông phát tiếng kêu", "Tiếng kêu lớn khi máy nén khởi động.", Priority.NORMAL, "PHONE", customerService.getUsername());

        WorkOrder wo1 = demoDataFactory.workOrder(tenant, sr1, customers.get(0), asset1, technician, "Kiểm tra hệ thống lạnh", Priority.HIGH,
                WorkOrderStatus.ASSIGNED, Instant.now().plus(2, ChronoUnit.HOURS), Instant.now().plus(4, ChronoUnit.HOURS));
        WorkOrder wo2 = demoDataFactory.workOrder(tenant, sr2, customers.get(1), asset2, technician2, "Bảo trì máy lạnh định kỳ", Priority.NORMAL,
                WorkOrderStatus.IN_PROGRESS, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS));
        WorkOrder wo3 = demoDataFactory.workOrder(tenant, sr3, customers.get(2), asset3, null, "Khắc phục tủ lạnh đóng tuyết", Priority.URGENT,
                WorkOrderStatus.OPEN, null, null);
        WorkOrder wo4 = demoDataFactory.workOrder(tenant, sr4, customers.get(3), asset4, technician, "Vệ sinh hệ thống điều hòa văn phòng", Priority.NORMAL,
                WorkOrderStatus.COMPLETED, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(2, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS));
        wo4.setDiagnosis("Dàn lạnh bám bụi, lưu lượng gió giảm.");
        wo4.setResolution("Vệ sinh dàn lạnh, kiểm tra dòng và áp suất gas.");
        wo4.setCompletedAt(Instant.now().minus(2, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS));

        demoDataFactory.appointment(tenant, wo1, technician);
        demoDataFactory.appointment(tenant, wo2, technician2);
        demoDataFactory.appointment(tenant, wo4, technician);

        demoDataFactory.history(tenant, wo1, null, WorkOrderStatus.OPEN, "Tạo từ yêu cầu dịch vụ", customerService.getUsername());
        demoDataFactory.history(tenant, wo1, WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, "Phân công kỹ thuật viên", dispatcher.getUsername());
        demoDataFactory.history(tenant, wo2, null, WorkOrderStatus.OPEN, "Tạo từ yêu cầu dịch vụ", customerService.getUsername());
        demoDataFactory.history(tenant, wo2, WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, "Phân công kỹ thuật viên", dispatcher.getUsername());
        demoDataFactory.history(tenant, wo2, WorkOrderStatus.ASSIGNED, WorkOrderStatus.ON_THE_WAY, "Đang di chuyển", technician2User.getUsername());
        demoDataFactory.history(tenant, wo2, WorkOrderStatus.ON_THE_WAY, WorkOrderStatus.IN_PROGRESS, "Bắt đầu công việc", technician2User.getUsername());
        demoDataFactory.history(tenant, wo3, null, WorkOrderStatus.OPEN, "Tạo từ yêu cầu dịch vụ", customerService.getUsername());
        demoDataFactory.history(tenant, wo4, null, WorkOrderStatus.OPEN, "Tạo từ yêu cầu dịch vụ", customerService.getUsername());
        demoDataFactory.history(tenant, wo4, WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, "Phân công kỹ thuật viên", dispatcher.getUsername());
        demoDataFactory.history(tenant, wo4, WorkOrderStatus.ASSIGNED, WorkOrderStatus.ON_THE_WAY, "Đang di chuyển", technicianUser.getUsername());
        demoDataFactory.history(tenant, wo4, WorkOrderStatus.ON_THE_WAY, WorkOrderStatus.IN_PROGRESS, "Bắt đầu công việc", technicianUser.getUsername());
        demoDataFactory.history(tenant, wo4, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.COMPLETED, "Hoàn tất công việc", technicianUser.getUsername());

        demoDataFactory.sparePart(tenant, "GAS-R32-1KG", "Gas lạnh R32", "kg", new BigDecimal("12.500"), new BigDecimal("3.000"), new BigDecimal("285000"));
        demoDataFactory.sparePart(tenant, "CAP-35UF", "Tụ điện 35µF", "cái", new BigDecimal("8"), new BigDecimal("3"), new BigDecimal("145000"));
        demoDataFactory.sparePart(tenant, "FILTER-AC-01", "Lưới lọc máy lạnh tiêu chuẩn", "cái", new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("95000"));
        demoDataFactory.sparePart(tenant, "SENSOR-TEMP-10K", "Cảm biến nhiệt độ 10K", "cái", new BigDecimal("15"), new BigDecimal("5"), new BigDecimal("120000"));
        demoDataFactory.sparePart(tenant, "COPPER-6-10", "Ống đồng 6/10", "m", new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("85000"));

        Notification notification = new Notification();
        notification.setTenantId(tenant.getId());
        notification.setRecipient(technicianUser);
        notification.setTitle("Công việc mới: " + wo1.getCode());
        notification.setMessage("Bạn được phân công kiểm tra máy lạnh tại Công ty TNHH An Phát.");
        notificationRepository.save(notification);

        auditService.recordAs(tenant.getId(), "system", "SEED", "SYSTEM", tenant.getId(), "Khởi tạo dữ liệu demo local-first");
    }

}
