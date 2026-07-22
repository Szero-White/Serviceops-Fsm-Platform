package com.serviceops.inventory.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "spare_parts")
public class SparePart extends TenantScopedEntity {
    @Column(nullable = false, length = 60)
    private String sku;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(name = "stock_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(name = "reorder_level", nullable = false, precision = 18, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    public void consume(BigDecimal quantity) {
        validateQuantity(quantity);
        if (stockQuantity.compareTo(quantity) < 0) {
            throw new IllegalStateException("Không đủ tồn kho");
        }
        stockQuantity = stockQuantity.subtract(quantity);
    }

    public void addStock(BigDecimal quantity) {
        validateQuantity(quantity);
        stockQuantity = stockQuantity.add(quantity);
    }

    private static void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
    }
}
