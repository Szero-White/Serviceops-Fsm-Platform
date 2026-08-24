package com.serviceops.security;

import com.serviceops.ai.web.AiController;
import com.serviceops.asset.web.AssetController;
import com.serviceops.customer.web.CustomerController;
import com.serviceops.dashboard.web.DashboardController;
import com.serviceops.servicerequest.web.ServiceRequestController;
import com.serviceops.technician.web.TechnicianController;
import com.serviceops.workorder.web.WorkOrderController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RoleOwnershipContractTest {

    private static final String MASTER_DATA_READ = "hasAnyRole('OWNER','CUSTOMER_SERVICE','DISPATCHER')";
    private static final String INTAKE_OWNERS = "hasAnyRole('OWNER','CUSTOMER_SERVICE')";
    private static final String OPERATIONAL_WORK_ORDER_READ = "hasAnyRole('OWNER','DISPATCHER','CUSTOMER_SERVICE','TECHNICIAN')";

    @Test
    void dispatcherCanReadCustomerAndAssetMasterDataButCannotOwnTheirWrites() {
        assertClassAuthorization(CustomerController.class, MASTER_DATA_READ);
        assertClassAuthorization(AssetController.class, MASTER_DATA_READ);

        for (String method : new String[]{"export", "importTemplate", "importCsv", "create", "update", "delete"}) {
            assertMethodAuthorization(CustomerController.class, method, INTAKE_OWNERS);
            assertMethodAuthorization(AssetController.class, method, INTAKE_OWNERS);
        }
    }

    @Test
    void serviceRequestIntakeAndAiDraftBelongToCustomerServiceAndOwner() {
        assertClassAuthorization(ServiceRequestController.class, INTAKE_OWNERS);
        assertMethodAuthorization(AiController.class, "draftServiceRequest", INTAKE_OWNERS);
    }

    @Test
    void workOrdersCanOnlyBeCreatedFromServiceRequestsAndCustomerServiceKeepsCancellationAccess() {
        assertClassAuthorization(WorkOrderController.class, OPERATIONAL_WORK_ORDER_READ);
        assertThat(Arrays.stream(WorkOrderController.class.getDeclaredMethods()).map(Method::getName))
                .doesNotContain("create");

        assertMethodAuthorization(WorkOrderController.class, "convert", INTAKE_OWNERS);
        assertMethodAuthorization(
                WorkOrderController.class,
                "schedule",
                "hasAnyRole('OWNER','DISPATCHER')"
        );
        assertMethodAuthorization(
                WorkOrderController.class,
                "transition",
                "hasAnyRole('OWNER','DISPATCHER','CUSTOMER_SERVICE','TECHNICIAN')"
        );
        assertMethodAuthorization(WorkOrderController.class, "deleteFromHistory", "hasRole('OWNER')");
    }

    @Test
    void dispatcherCanReadTechnicianProfilesButOnlyOwnerCanEditThem() {
        assertClassAuthorization(TechnicianController.class, "hasAnyRole('OWNER','DISPATCHER')");
        assertMethodAuthorization(TechnicianController.class, "updateProfile", "hasRole('OWNER')");
    }

    @Test
    void operationalDashboardExcludesWarehouse() {
        assertClassAuthorization(DashboardController.class, OPERATIONAL_WORK_ORDER_READ);
    }

    private static void assertClassAuthorization(Class<?> controller, String expected) {
        PreAuthorize annotation = controller.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("%s class authorization", controller.getSimpleName())
                .isNotNull();
        assertThat(annotation.value()).isEqualTo(expected);
    }

    private static void assertMethodAuthorization(Class<?> controller, String methodName, String expected) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Method %s.%s was not found".formatted(controller.getSimpleName(), methodName)
                ));

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("%s.%s authorization", controller.getSimpleName(), methodName)
                .isNotNull();
        assertThat(annotation.value()).isEqualTo(expected);
    }
}
