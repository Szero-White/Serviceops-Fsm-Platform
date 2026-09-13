package com.serviceops.common.businesscode;

public enum BusinessCodeType {
    CUSTOMER("KH"),
    WORK_ORDER("WO"),
    PAYMENT_RECEIPT("BN"),
    SPARE_PART("PT");

    private final String prefix;

    BusinessCodeType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
