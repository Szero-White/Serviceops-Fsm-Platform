package com.serviceops.inventory.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SparePartTest {

    @Test
    void shouldDecreaseAvailableStock() {
        SparePart part = partWithStock("10.000");

        part.decreaseStock(new BigDecimal("2.500"));

        assertThat(part.getStockQuantity()).isEqualByComparingTo("7.500");
    }

    @Test
    void shouldAllowDecreasingExactAvailableStock() {
        SparePart part = partWithStock("2.000");

        part.decreaseStock(new BigDecimal("2.000"));

        assertThat(part.getStockQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRejectInsufficientStockWithoutMutatingBalance() {
        SparePart part = partWithStock("2.000");

        assertThatThrownBy(() -> part.decreaseStock(new BigDecimal("3.000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Không đủ tồn kho");
        assertThat(part.getStockQuantity()).isEqualByComparingTo("2.000");
    }

    @Test
    void shouldRejectNonPositiveDecreaseQuantity() {
        SparePart part = partWithStock("10");

        assertThatThrownBy(() -> part.decreaseStock(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> part.decreaseStock(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> part.decreaseStock(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAddStockUsingDecimalArithmetic() {
        SparePart part = partWithStock("1.250");

        part.addStock(new BigDecimal("0.750"));

        assertThat(part.getStockQuantity()).isEqualByComparingTo("2.000");
    }

    @Test
    void shouldRejectNonPositiveImportQuantityWithoutMutatingBalance() {
        SparePart part = partWithStock("4.000");

        assertThatThrownBy(() -> part.addStock(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(part.getStockQuantity()).isEqualByComparingTo("4.000");
    }

    private static SparePart partWithStock(String quantity) {
        SparePart part = new SparePart();
        part.setStockQuantity(new BigDecimal(quantity));
        return part;
    }
}
