package com.serviceops.scheduling.application;

import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.scheduling.domain.Appointment;
import com.serviceops.scheduling.domain.AppointmentRepository;
import com.serviceops.scheduling.domain.AppointmentStatus;
import com.serviceops.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OverdueWorkOrderNotificationService {
    private static final List<UserRole> DISPATCHER_ROLES = List.of(UserRole.DISPATCHER);

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Transactional
    public int notifyOverdueAppointments(Instant now) {
        int created = 0;
        for (Appointment appointment : appointmentRepository.findOverdueNotificationCandidates(
                now,
                AppointmentStatus.ACTIVE
        )) {
            WorkOrder workOrder = appointment.getWorkOrder();
            String eventKey = overdueEventKey(appointment);
            var context = notificationContext(workOrder);

            var dispatcherCopy = NotificationCopy.workOrderOverdueForDispatcher(
                    context,
                    appointment.getTechnician().getUser().getDisplayName(),
                    appointment.getStartTime(),
                    appointment.getEndTime()
            );
            created += notificationService.notifyRolesUnique(
                    workOrder.getTenantId(),
                    DISPATCHER_ROLES,
                    eventKey,
                    dispatcherCopy.title(),
                    dispatcherCopy.message()
            );

            if (appointment.getTechnician().isActive() && appointment.getTechnician().getUser().isActive()) {
                var technicianCopy = NotificationCopy.workOrderOverdueForTechnician(
                        context,
                        appointment.getStartTime(),
                        appointment.getEndTime()
                );
                if (notificationService.createUnique(
                        workOrder.getTenantId(),
                        appointment.getTechnician().getUser(),
                        eventKey,
                        technicianCopy.title(),
                        technicianCopy.message()
                )) {
                    created++;
                }
            }
        }
        return created;
    }

    private static String overdueEventKey(Appointment appointment) {
        return "WORK_ORDER_OVERDUE:" + appointment.getId()
                + ":" + appointment.getStartTime().getEpochSecond()
                + ":" + appointment.getEndTime().getEpochSecond();
    }

    private static NotificationCopy.WorkOrderContext notificationContext(WorkOrder workOrder) {
        return new NotificationCopy.WorkOrderContext(
                workOrder.getCode(),
                workOrder.getSummary(),
                workOrder.getCustomer().getName()
        );
    }
}
