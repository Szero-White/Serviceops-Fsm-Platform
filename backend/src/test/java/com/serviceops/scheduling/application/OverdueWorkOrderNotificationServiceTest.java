package com.serviceops.scheduling.application;

import com.serviceops.common.domain.Priority;
import com.serviceops.customer.domain.Customer;
import com.serviceops.identity.domain.UserAccount;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.scheduling.domain.Appointment;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.scheduling.domain.AppointmentStatus;
import com.serviceops.technician.domain.TechnicianProfile;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueWorkOrderNotificationServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-08-26T03:11:00Z");
    private static final Instant END = Instant.parse("2026-08-26T03:12:00Z");
    private static final Instant NOW = Instant.parse("2026-08-26T03:15:00Z");
    private static final Instant AFTER_CUSTOMER_SERVICE_GRACE = Instant.parse("2026-08-26T03:28:00Z");

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void overdueAppointmentAlertsDispatcherAndAssignedTechnicianWithOneStableEventKey() {
        Appointment appointment = overdueAppointment(true);
        when(appointmentRepository.findOverdueNotificationCandidates(NOW, AppointmentStatus.ACTIVE))
                .thenReturn(List.of(appointment));
        when(notificationService.notifyRolesUnique(
                eq(TENANT_ID),
                eq(List.of(UserRole.DISPATCHER)),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(1);
        when(notificationService.createUnique(
                eq(TENANT_ID),
                eq(appointment.getTechnician().getUser()),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(true);

        OverdueWorkOrderNotificationService service = serviceWithGrace(Duration.ofMinutes(15));

        int created = service.notifyOverdueAppointments(NOW);

        assertThat(created).isEqualTo(2);

        ArgumentCaptor<String> dispatcherEventKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dispatcherTitle = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dispatcherMessage = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyRolesUnique(
                eq(TENANT_ID),
                eq(List.of(UserRole.DISPATCHER)),
                dispatcherEventKey.capture(),
                dispatcherTitle.capture(),
                dispatcherMessage.capture()
        );

        ArgumentCaptor<String> technicianEventKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> technicianTitle = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> technicianMessage = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createUnique(
                eq(TENANT_ID),
                eq(appointment.getTechnician().getUser()),
                technicianEventKey.capture(),
                technicianTitle.capture(),
                technicianMessage.capture()
        );

        assertThat(dispatcherEventKey.getValue())
                .isEqualTo(technicianEventKey.getValue())
                .contains(appointment.getId().toString())
                .contains(String.valueOf(START.getEpochSecond()))
                .contains(String.valueOf(END.getEpochSecond()));
        assertThat(dispatcherTitle.getValue()).isEqualTo("Phiếu đã quá lịch thực hiện: WO-2026-001006");
        assertThat(dispatcherMessage.getValue())
                .contains("Công ty TNHH Cà Phê An Nhiên")
                .contains("Trịnh Quốc Tiến")
                .contains("26/08/2026 10:11–10:12")
                .contains("Lịch điều phối");
        assertThat(technicianTitle.getValue()).isEqualTo("Công việc đã quá lịch: WO-2026-001006");
        assertThat(technicianMessage.getValue())
                .contains("Công ty TNHH Cà Phê An Nhiên")
                .contains("26/08/2026 10:11–10:12")
                .contains("Lịch của tôi");
        verify(notificationService, never()).notifyRolesUnique(
                eq(TENANT_ID),
                eq(List.of(UserRole.CUSTOMER_SERVICE)),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void overdueBeyondGraceEscalatesOnceToCustomerServiceWithSeparateEventKey() {
        Appointment appointment = overdueAppointment(true);
        when(appointmentRepository.findOverdueNotificationCandidates(
                AFTER_CUSTOMER_SERVICE_GRACE,
                AppointmentStatus.ACTIVE
        )).thenReturn(List.of(appointment));
        when(notificationService.notifyRolesUnique(
                eq(TENANT_ID),
                eq(List.of(UserRole.DISPATCHER)),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(0);
        when(notificationService.createUnique(
                eq(TENANT_ID),
                eq(appointment.getTechnician().getUser()),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(false);
        when(notificationService.notifyRolesUnique(
                eq(TENANT_ID),
                eq(List.of(UserRole.CUSTOMER_SERVICE)),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(1);

        OverdueWorkOrderNotificationService service = serviceWithGrace(Duration.ofMinutes(15));

        assertThat(service.notifyOverdueAppointments(AFTER_CUSTOMER_SERVICE_GRACE)).isEqualTo(1);

        ArgumentCaptor<String> eventKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyRolesUnique(
                eq(TENANT_ID),
                eq(List.of(UserRole.CUSTOMER_SERVICE)),
                eventKey.capture(),
                title.capture(),
                message.capture()
        );

        assertThat(eventKey.getValue())
                .contains(appointment.getId().toString())
                .endsWith(":CUSTOMER_SERVICE");
        assertThat(title.getValue()).isEqualTo("Khách hàng có thể cần được liên hệ: WO-2026-001006");
        assertThat(message.getValue())
                .contains("Công ty TNHH Cà Phê An Nhiên")
                .contains("Trịnh Quốc Tiến")
                .contains("26/08/2026 10:11–10:12")
                .contains("chủ động liên hệ khách hàng");
    }

    @Test
    void inactiveTechnicianDoesNotReceiveOverdueAlertButDispatcherStillDoes() {
        Appointment appointment = overdueAppointment(false);
        when(appointmentRepository.findOverdueNotificationCandidates(NOW, AppointmentStatus.ACTIVE))
                .thenReturn(List.of(appointment));
        when(notificationService.notifyRolesUnique(
                eq(TENANT_ID),
                eq(List.of(UserRole.DISPATCHER)),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(1);

        OverdueWorkOrderNotificationService service = serviceWithGrace(Duration.ofMinutes(15));

        assertThat(service.notifyOverdueAppointments(NOW)).isEqualTo(1);
        verify(notificationService, never()).createUnique(
                eq(TENANT_ID),
                eq(appointment.getTechnician().getUser()),
                anyString(),
                anyString(),
                anyString()
        );
    }

    private OverdueWorkOrderNotificationService serviceWithGrace(Duration grace) {
        OverdueNotificationProperties properties = new OverdueNotificationProperties();
        properties.setCustomerServiceGrace(grace);
        return new OverdueWorkOrderNotificationService(appointmentRepository, notificationService, properties);
    }

    private static Appointment overdueAppointment(boolean activeTechnician) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(TENANT_ID);
        customer.setCode("KH-56513");
        customer.setName("Công ty TNHH Cà Phê An Nhiên");
        customer.setActive(true);

        UserAccount technicianUser = new UserAccount();
        technicianUser.setId(UUID.randomUUID());
        technicianUser.setTenantId(TENANT_ID);
        technicianUser.setUsername("technician2");
        technicianUser.setDisplayName("Trịnh Quốc Tiến");
        technicianUser.setPasswordHash("test");
        technicianUser.setRole(UserRole.TECHNICIAN);
        technicianUser.setActive(activeTechnician);

        TechnicianProfile technician = new TechnicianProfile();
        technician.setId(UUID.randomUUID());
        technician.setTenantId(TENANT_ID);
        technician.setUser(technicianUser);
        technician.setActive(activeTechnician);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(UUID.randomUUID());
        workOrder.setTenantId(TENANT_ID);
        workOrder.setCustomer(customer);
        workOrder.setTechnician(technician);
        workOrder.setCode("WO-2026-001006");
        workOrder.setSummary("Máy rửa chén không cấp nước");
        workOrder.setPriority(Priority.HIGH);
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        workOrder.setScheduledStart(START);
        workOrder.setScheduledEnd(END);

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setTenantId(TENANT_ID);
        appointment.setWorkOrder(workOrder);
        appointment.setTechnician(technician);
        appointment.setStartTime(START);
        appointment.setEndTime(END);
        appointment.setStatus(AppointmentStatus.ACTIVE);
        return appointment;
    }
}
