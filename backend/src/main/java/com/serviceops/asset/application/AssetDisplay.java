package com.serviceops.asset.application;

import com.serviceops.asset.domain.Asset;

/** Shared human-readable asset presentation used by service/work-order read models. */
public final class AssetDisplay {
    private AssetDisplay() {
    }

    public static String label(Asset asset) {
        String equipmentName = ((asset.getBrand() == null ? "" : asset.getBrand() + " ")
                + (asset.getModel() == null ? "" : asset.getModel())).trim();
        if (equipmentName.isBlank()) {
            equipmentName = asset.getCategory();
        }
        String serial = asset.getSerialNumber() == null ? "Chưa xác định số sê-ri" : asset.getSerialNumber();
        return equipmentName + " (" + serial + ")";
    }
}
