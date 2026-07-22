package com.serviceops.inventory.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SparePartTest {

    @Test
    void shouldConsumeAvailableStock() {
        SparePart part = new SparePart();
        part.setStockQuantity(new BigDecimal("10.000"));

        part.consume(new BigDecimal("2.500"));

        assertThat(part.getStockQuantity()).isEqualByComparingTo("7.500");
    }

    @Test
    void shouldRejectNegativeStock() {
        SparePart part = new SparePart();
        part.setStockQuantity(new BigDecimal("2.000"));

        assertThatThrownBy(() -> part.consume(new BigDecimal("3.000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Không đủ tồn kho");
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        SparePart part = new SparePart();
        part.setStockQuantity(BigDecimal.TEN);

        assertThatThrownBy(() -> part.consume(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
