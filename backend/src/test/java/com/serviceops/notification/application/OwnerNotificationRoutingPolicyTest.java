package com.serviceops.notification.application;

import com.serviceops.asset.application.AssetService;
import com.serviceops.attachment.application.AttachmentService;
import com.serviceops.customer.application.CustomerService;
import com.serviceops.inventory.application.InventoryService;
import com.serviceops.servicerequest.application.ServiceChannelService;
import com.serviceops.servicerequest.application.ServiceRequestService;
import com.serviceops.technician.application.TechnicianService;
import com.serviceops.workorder.application.WorkOrderService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerNotificationRoutingPolicyTest {

    @Test
    void routineCrudServicesDoNotOwnBellNotificationBroadcasters() {
        assertNoNotificationService(CustomerService.class);
        assertNoNotificationService(AssetService.class);
        assertNoNotificationService(ServiceRequestService.class);
        assertNoNotificationService(ServiceChannelService.class);
        assertNoNotificationService(TechnicianService.class);
        assertNoNotificationService(AttachmentService.class);
    }

    @Test
    void attentionAndAssignmentDomainsKeepNotificationRouting() {
        assertHasNotificationService(WorkOrderService.class);
        assertHasNotificationService(InventoryService.class);
    }

    private static void assertNoNotificationService(Class<?> type) {
        assertThat(Arrays.stream(type.getDeclaredFields())
                .noneMatch(field -> field.getType().equals(NotificationService.class)))
                .as(type.getSimpleName() + " should rely on UI feedback/audit instead of routine bell broadcasts")
                .isTrue();
    }

    private static void assertHasNotificationService(Class<?> type) {
        assertThat(Arrays.stream(type.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(NotificationService.class)))
                .as(type.getSimpleName() + " should keep actionable notification routing")
                .isTrue();
    }
}
