package com.serviceops.integration.support;

import com.serviceops.asset.domain.Asset;
import com.serviceops.asset.domain.AssetStatus;
import com.serviceops.common.domain.Priority;
import com.serviceops.customer.domain.Customer;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderStatus;

import java.util.Locale;
import java.util.UUID;

public final class IntegrationTestFixtures {

    private static final int CODE_MAX_LENGTH = 40;
    private static final int RANDOM_SUFFIX_LENGTH = 8;

    private IntegrationTestFixtures() {
    }

    public static String uniqueCode(String prefix) {
        String suffix = UUID.randomUUID()
                .toString()
                .substring(0, RANDOM_SUFFIX_LENGTH)
                .toUpperCase(Locale.ROOT);

        if (prefix.length() + suffix.length() > CODE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Integration-test code prefix must leave room for an 8-character suffix"
            );
        }

        return prefix + suffix;
    }

    public static String shortId() {
        return UUID.randomUUID().toString().substring(0, RANDOM_SUFFIX_LENGTH);
    }

    public static Customer customer(UUID tenantId, String code, String name) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setCode(code);
        customer.setName(name);
        customer.setActive(true);
        return customer;
    }

    public static Asset asset(UUID tenantId, Customer customer, String serialNumber) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomer(customer);
        asset.setCategory("Laptop");
        asset.setSerialNumber(serialNumber);
        asset.setStatus(AssetStatus.ACTIVE);
        return asset;
    }

    public static WorkOrder workOrder(UUID tenantId, Customer customer, String code) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTenantId(tenantId);
        workOrder.setCustomer(customer);
        workOrder.setCode(code);
        workOrder.setSummary("Integration test work order");
        workOrder.setPriority(Priority.NORMAL);
        workOrder.setStatus(WorkOrderStatus.OPEN);
        return workOrder;
    }
}
