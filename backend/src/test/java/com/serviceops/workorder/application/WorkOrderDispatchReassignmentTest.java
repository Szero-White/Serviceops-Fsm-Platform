package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.customer.domain.Customer;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.scheduling.domain.Appointment;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.servicerequest.domain.ServiceRequestRepository;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.technician.domain.TechnicianRepository;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderDtos.ScheduleWorkOrder;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderDispatchReassignmentTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID DISPATCHER_ID = UUID.randomUUID();
    private static final UUID WORK_ORDER_ID = UUID.randomUUID();

    @Mock private WorkOrderRepository repository;
    @Mock private WorkOrderStatusHistoryRepository historyRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private WorkOrderService service;
    private WorkOrder workOrder;
    private TechnicianProfile previousTechnician;
    private TechnicianProfile replacementTechnician;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        authenticateDispatcher();
        service = new WorkOrderService(
                repository,
                historyRepository,
                inventoryTransactionRepository,
                serviceRequestRepository,
                technicianRepository,
                appointmentRepository,
                auditService,
                notificationService
        );

        previousTechnician = technician("Kỹ thuật viên A");
        replacementTechnician = technician("Kỹ thuật viên B");

        Instant oldStart = Instant.parse("2026-08-25T02:00:00Z");
        Instant oldEnd = Instant.parse("2026-08-25T04:00:00Z");

        workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setTenantId(TENANT_ID);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setName("Công ty TNHH An Phát");

        workOrder.setCode("WO-2026-001006");
        workOrder.setSummary("Máy rửa chén không cấp nước");
        workOrder.setCustomer(customer);
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        workOrder.setTechnician(previousTechnician);
        workOrder.setScheduledStart(oldStart);
        workOrder.setScheduledEnd(oldEnd);

        appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setTenantId(TENANT_ID);
        appointment.setWorkOrder(workOrder);
        appointment.setTechnician(previousTechnician);
        appointment.setStartTime(oldStart);
        appointment.setEndTime(oldEnd);

        when(repository.findForUpdate(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void initialAssignmentNotifiesTechnicianWithProfessionalHandoffContext() {
        Instant start = Instant.parse("2026-08-25T05:00:00Z");
        Instant end = Instant.parse("2026-08-25T07:00:00Z");
        workOrder.setStatus(WorkOrderStatus.OPEN);
        workOrder.setTechnician(null);
        workOrder.setScheduledStart(null);
        workOrder.setScheduledEnd(null);

        when(technicianRepository.findForUpdate(replacementTechnician.getId(), TENANT_ID))
                .thenReturn(Optional.of(replacementTechnician));
        when(appointmentRepository.findByTenantIdAndWorkOrderId(TENANT_ID, WORK_ORDER_ID))
                .thenReturn(Optional.empty());
        when(appointmentRepository.existsOverlap(
                TENANT_ID,
                replacementTechnician.getId(),
                start,
                end,
                com.serviceops.scheduling.domain.AppointmentStatus.ACTIVE,
                WORK_ORDER_ID
        )).thenReturn(false);
        stubSuccessfulResponseRead();

        service.schedule(
                WORK_ORDER_ID,
                new ScheduleWorkOrder(replacementTechnician.getId(), start, end, null)
        );

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(workOrder.getTechnician().getId()).isEqualTo(replacementTechnician.getId());
        verify(auditService).record(
                eq("ASSIGN"),
                eq("WORK_ORDER"),
                eq(WORK_ORDER_ID),
                org.mockito.ArgumentMatchers.contains("Kỹ thuật viên B")
        );
        verify(notificationService).create(
                eq(TENANT_ID),
                eq(replacementTechnician.getUser()),
                eq("Bạn có công việc mới: WO-2026-001006"),
                org.mockito.ArgumentMatchers.argThat(message ->
                        message.contains("Điều phối viên Lê Thu Điều phối")
                                && message.contains("Máy rửa chén không cấp nước")
                                && message.contains("Công ty TNHH An Phát")
                                && message.contains("Lịch của tôi"))
        );
    }

    @Test
    void dispatcherCanReassignBeforeFieldWorkStartsAndBothTechniciansAreNotified() {
        Instant newStart = Instant.parse("2026-08-25T05:00:00Z");
        Instant newEnd = Instant.parse("2026-08-25T07:00:00Z");
        when(technicianRepository.findForUpdate(replacementTechnician.getId(), TENANT_ID))
                .thenReturn(Optional.of(replacementTechnician));
        when(appointmentRepository.findByTenantIdAndWorkOrderId(TENANT_ID, WORK_ORDER_ID))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsOverlap(
                TENANT_ID,
                replacementTechnician.getId(),
                newStart,
                newEnd,
                com.serviceops.scheduling.domain.AppointmentStatus.ACTIVE,
                WORK_ORDER_ID
        )).thenReturn(false);

        stubSuccessfulResponseRead();

        service.schedule(
                WORK_ORDER_ID,
                new ScheduleWorkOrder(
                        replacementTechnician.getId(),
                        newStart,
                        newEnd,
                        "Kỹ thuật viên hiện tại chưa thể bắt đầu đúng lịch; cần đáp ứng khách hàng nhanh hơn"
                )
        );

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(workOrder.getTechnician().getId()).isEqualTo(replacementTechnician.getId());
        assertThat(appointment.getTechnician().getId()).isEqualTo(replacementTechnician.getId());

        verify(auditService).record(
                eq("RESCHEDULE"),
                eq("WORK_ORDER"),
                eq(WORK_ORDER_ID),
                org.mockito.ArgumentMatchers.argThat(details ->
                        details.contains("Kỹ thuật viên A [2026-08-25T02:00:00Z - 2026-08-25T04:00:00Z]")
                                && details.contains("→ Kỹ thuật viên B [2026-08-25T05:00:00Z - 2026-08-25T07:00:00Z]")
                                && details.contains("Lý do: Kỹ thuật viên hiện tại chưa thể bắt đầu đúng lịch"))
        );
        verify(notificationService).create(
                eq(TENANT_ID),
                eq(previousTechnician.getUser()),
                eq("Bạn không còn phụ trách: WO-2026-001006"),
                org.mockito.ArgumentMatchers.argThat(message ->
                        message.contains("Kỹ thuật viên B")
                                && message.contains("Công ty TNHH An Phát")
                                && message.contains("Lịch của tôi"))
        );
        verify(notificationService).create(
                eq(TENANT_ID),
                eq(replacementTechnician.getUser()),
                eq("Bạn có công việc mới: WO-2026-001006"),
                org.mockito.ArgumentMatchers.argThat(message ->
                        message.contains("Điều phối viên Lê Thu Điều phối")
                                && message.contains("Công ty TNHH An Phát")
                                && message.contains("Lịch của tôi"))
        );
    }


    @Test
    void dispatcherCanRescheduleSameTechnicianAndOnlyCurrentTechnicianIsNotified() {
        Instant newStart = Instant.parse("2026-08-25T06:00:00Z");
        Instant newEnd = Instant.parse("2026-08-25T08:00:00Z");
        when(technicianRepository.findForUpdate(previousTechnician.getId(), TENANT_ID))
                .thenReturn(Optional.of(previousTechnician));
        when(appointmentRepository.findByTenantIdAndWorkOrderId(TENANT_ID, WORK_ORDER_ID))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsOverlap(
                TENANT_ID,
                previousTechnician.getId(),
                newStart,
                newEnd,
                com.serviceops.scheduling.domain.AppointmentStatus.ACTIVE,
                WORK_ORDER_ID
        )).thenReturn(false);
        stubSuccessfulResponseRead();

        service.schedule(
                WORK_ORDER_ID,
                new ScheduleWorkOrder(
                        previousTechnician.getId(),
                        newStart,
                        newEnd,
                        "Khách hàng đề nghị dời khung giờ tiếp nhận"
                )
        );

        assertThat(workOrder.getTechnician().getId()).isEqualTo(previousTechnician.getId());
        assertThat(workOrder.getScheduledStart()).isEqualTo(newStart);
        assertThat(appointment.getStartTime()).isEqualTo(newStart);

        verify(auditService).record(
                eq("RESCHEDULE"),
                eq("WORK_ORDER"),
                eq(WORK_ORDER_ID),
                org.mockito.ArgumentMatchers.argThat(details ->
                        details.contains("Kỹ thuật viên A [2026-08-25T02:00:00Z - 2026-08-25T04:00:00Z]")
                                && details.contains("→ Kỹ thuật viên A [2026-08-25T06:00:00Z - 2026-08-25T08:00:00Z]")
                                && details.contains("Lý do: Khách hàng đề nghị dời khung giờ tiếp nhận"))
        );
        var expectedNotification = NotificationCopy.technicianScheduleChanged(
                new NotificationCopy.WorkOrderContext(
                        "WO-2026-001006",
                        "Máy rửa chén không cấp nước",
                        "Công ty TNHH An Phát"
                ),
                "Điều phối viên Lê Thu Điều phối",
                Instant.parse("2026-08-25T02:00:00Z"),
                Instant.parse("2026-08-25T04:00:00Z"),
                newStart,
                newEnd,
                "Khách hàng đề nghị dời khung giờ tiếp nhận"
        );
        verify(notificationService).create(
                eq(TENANT_ID),
                eq(previousTechnician.getUser()),
                eq(expectedNotification.title()),
                eq(expectedNotification.message())
        );
        verify(notificationService, never()).create(
                eq(TENANT_ID),
                eq(replacementTechnician.getUser()),
                any(),
                any()
        );
    }

    @Test
    void unchangedRedispatchIsNoOpWithoutAuditOrNotification() {
        when(technicianRepository.findForUpdate(previousTechnician.getId(), TENANT_ID))
                .thenReturn(Optional.of(previousTechnician));
        when(appointmentRepository.findByTenantIdAndWorkOrderId(TENANT_ID, WORK_ORDER_ID))
                .thenReturn(Optional.of(appointment));
        stubSuccessfulResponseRead();

        service.schedule(
                WORK_ORDER_ID,
                new ScheduleWorkOrder(
                        previousTechnician.getId(),
                        appointment.getStartTime(),
                        appointment.getEndTime(),
                        null
                )
        );

        verify(appointmentRepository, never()).existsOverlap(any(), any(), any(), any(), any(), any());
        verify(appointmentRepository, never()).save(any());
        verify(auditService, never()).record(eq("RESCHEDULE"), eq("WORK_ORDER"), eq(WORK_ORDER_ID), any());
        verify(notificationService, never()).create(any(), any(), any(), any());
    }

    @Test
    void cannotRedispatchAfterTechnicianHasStartedFieldWork() {
        workOrder.setStatus(WorkOrderStatus.ON_THE_WAY);

        assertThatThrownBy(() -> service.schedule(
                WORK_ORDER_ID,
                new ScheduleWorkOrder(
                        previousTechnician.getId(),
                        appointment.getStartTime(),
                        appointment.getEndTime(),
                        "Thử điều phối sau khi đã bắt đầu"
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trước khi kỹ thuật viên bắt đầu");

        verify(technicianRepository, never()).findForUpdate(any(), any());
        verify(appointmentRepository, never()).save(any());
        verify(auditService, never()).record(eq("RESCHEDULE"), eq("WORK_ORDER"), eq(WORK_ORDER_ID), any());
    }

    @Test
    void redispatchRequiresReason() {
        Instant newStart = Instant.parse("2026-08-25T05:00:00Z");
        Instant newEnd = Instant.parse("2026-08-25T07:00:00Z");
        when(technicianRepository.findForUpdate(replacementTechnician.getId(), TENANT_ID))
                .thenReturn(Optional.of(replacementTechnician));
        when(appointmentRepository.findByTenantIdAndWorkOrderId(TENANT_ID, WORK_ORDER_ID))
                .thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.schedule(
                WORK_ORDER_ID,
                new ScheduleWorkOrder(replacementTechnician.getId(), newStart, newEnd, " ")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("lý do");

        verify(appointmentRepository, never()).save(any());
        verify(auditService, never()).record(eq("RESCHEDULE"), eq("WORK_ORDER"), eq(WORK_ORDER_ID), any());
        verify(notificationService, never()).create(any(), any(), any(), any());
    }

    private void stubSuccessfulResponseRead() {
        when(repository.findDetailed(WORK_ORDER_ID, TENANT_ID)).thenReturn(Optional.of(workOrder));
        when(historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(TENANT_ID, WORK_ORDER_ID)).thenReturn(List.of());
        when(inventoryTransactionRepository.findPartUsageForWorkOrder(TENANT_ID, WORK_ORDER_ID)).thenReturn(List.of());
        when(auditService.findEntityEvents(WORK_ORDER_ID, "WORK_ORDER", List.of("RESCHEDULE"))).thenReturn(List.of());
    }

    private static TechnicianProfile technician(String displayName) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setTenantId(TENANT_ID);
        user.setUsername(displayName.replace(" ", ".").toLowerCase());
        user.setDisplayName(displayName);
        user.setRole(UserRole.TECHNICIAN);
        user.setActive(true);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(UUID.randomUUID());
        technician.setTenantId(TENANT_ID);
        technician.setUser(user);
        technician.setActive(true);
        return technician;
    }

    private static void authenticateDispatcher() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("dispatcher")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claims(claims -> claims.putAll(Map.of(
                        "tenantId", TENANT_ID.toString(),
                        "userId", DISPATCHER_ID.toString(),
                        "displayName", "Lê Thu Điều phối",
                        "roles", List.of("DISPATCHER")
                )))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
