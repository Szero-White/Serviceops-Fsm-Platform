package com.serviceops.security;

import com.serviceops.ai.web.AiController;
import com.serviceops.asset.web.AssetController;
import com.serviceops.audit.web.AuditController;
import com.serviceops.customer.web.CustomerController;
import com.serviceops.dashboard.web.DashboardController;
import com.serviceops.inventory.web.InventoryController;
import com.serviceops.inventory.web.WorkOrderPartController;
import com.serviceops.identity.web.UserManagementController;
import com.serviceops.payment.web.PaymentController;
import com.serviceops.payment.web.PaymentReceiptController;
import com.serviceops.servicerequest.web.ServiceChannelController;
import com.serviceops.servicerequest.web.ServiceRequestController;
import com.serviceops.technician.web.TechnicianController;
import com.serviceops.workorder.web.WorkOrderBillingController;
import com.serviceops.workorder.web.WorkOrderClosureController;
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
    void ownerKeepsAdministrativeCoverageWithoutImpersonatingTechnicianOnlyExecution() {
        assertClassAuthorization(UserManagementController.class, "hasRole('OWNER')");
        assertClassAuthorization(ServiceRequestController.class, INTAKE_OWNERS);
        assertClassAuthorization(ServiceChannelController.class, INTAKE_OWNERS);
        assertMethodAuthorization(WorkOrderController.class, "convert", INTAKE_OWNERS);
        assertMethodAuthorization(WorkOrderController.class, "schedule", "hasAnyRole('OWNER','DISPATCHER')");
        assertMethodAuthorization(WorkOrderController.class, "deleteFromHistory", "hasRole('OWNER')");
        assertMethodAuthorization(InventoryController.class, "stocktake", "hasAnyRole('OWNER','WAREHOUSE_STAFF')");
    }

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
    void warehouseOwnsStockReconciliationMovementAndPhysicalReturnsWhileLegacyConsumptionIsReadOnlyHistory() {
        String warehouseOwners = "hasAnyRole('OWNER','WAREHOUSE_STAFF')";
        assertMethodAuthorization(InventoryController.class, "updateReorderLevel", warehouseOwners);
        assertMethodAuthorization(InventoryController.class, "stocktake", warehouseOwners);
        assertMethodAuthorization(InventoryController.class, "transactions", warehouseOwners);
        assertMethodAuthorization(WorkOrderPartController.class, "outstandingParts", warehouseOwners);
        assertMethodAuthorization(WorkOrderPartController.class, "partRequests", warehouseOwners);
        assertMethodAuthorization(WorkOrderPartController.class, "createPartRequest", "hasRole('TECHNICIAN')");
        assertMethodAuthorization(WorkOrderPartController.class, "updatePartRequest", "hasRole('TECHNICIAN')");
        assertMethodAuthorization(WorkOrderPartController.class, "cancelPartRequest", "hasRole('TECHNICIAN')");
        assertMethodAuthorization(WorkOrderPartController.class, "markPartRequestUnavailable", "hasRole('WAREHOUSE_STAFF')");
        assertMethodAuthorization(WorkOrderPartController.class, "issuePartRequest", "hasRole('WAREHOUSE_STAFF')");
        assertMethodAuthorization(WorkOrderPartController.class, "updatePartUsage", "hasRole('TECHNICIAN')");
        assertMethodAuthorization(WorkOrderPartController.class, "returnable", warehouseOwners);
        assertMethodAuthorization(WorkOrderPartController.class, "returnPart", "hasRole('WAREHOUSE_STAFF')");
        assertThat(Arrays.stream(InventoryController.class.getDeclaredMethods()).map(Method::getName))
                .doesNotContain("consume");
    }

    @Test
    void billingPaymentReceiptAndClosureFollowTheApprovedRoleOwnership() {
        assertMethodAuthorization(
                WorkOrderBillingController.class,
                "billing",
                "hasAnyRole('OWNER','CUSTOMER_SERVICE','TECHNICIAN')"
        );
        assertMethodAuthorization(WorkOrderBillingController.class, "updateBilling", "hasRole('TECHNICIAN')");
        assertMethodAuthorization(WorkOrderBillingController.class, "customerAcceptance", "hasRole('TECHNICIAN')");

        assertMethodAuthorization(PaymentController.class, "payments", "hasAnyRole('OWNER','CUSTOMER_SERVICE')");
        assertMethodAuthorization(
                PaymentController.class,
                "workOrderPayment",
                "hasAnyRole('OWNER','CUSTOMER_SERVICE','TECHNICIAN')"
        );
        assertMethodAuthorization(PaymentController.class, "reportTransfer", "hasRole('TECHNICIAN')");
        assertMethodAuthorization(PaymentController.class, "collectCash", "hasRole('TECHNICIAN')");
        assertMethodAuthorization(PaymentController.class, "settleTransfer", "hasRole('CUSTOMER_SERVICE')");
        assertMethodAuthorization(PaymentController.class, "settleCash", "hasRole('CUSTOMER_SERVICE')");
        assertMethodAuthorization(
                PaymentController.class,
                "paymentProfile",
                "hasAnyRole('OWNER','CUSTOMER_SERVICE','TECHNICIAN')"
        );
        assertMethodAuthorization(PaymentController.class, "updatePaymentProfile", "hasRole('OWNER')");

        assertMethodAuthorization(PaymentReceiptController.class, "issue", "hasRole('CUSTOMER_SERVICE')");
        assertMethodAuthorization(
                PaymentReceiptController.class,
                "download",
                "hasAnyRole('OWNER','CUSTOMER_SERVICE')"
        );
        assertMethodAuthorization(WorkOrderClosureController.class, "close", "hasRole('CUSTOMER_SERVICE')");
    }


    @Test
    void systemAuditRemainsAnOwnerGovernanceCapability() {
        assertClassAuthorization(AuditController.class, "hasRole('OWNER')");
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
