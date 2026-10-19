package com.pucpr.tcc.monolith.orders.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa OrderItem isolado: o cálculo de subtotal (preço × quantidade) e o
 * snapshot de preço/nome no momento da compra, que é a garantia de domínio
 * documentada na classe (alterações futuras de preço não afetam pedidos já
 * feitos).
 */
class OrderItemTest {

    @Test
    @DisplayName("subtotal deve ser unitPrice multiplicado por quantity")
    void subtotalShouldMultiplyPriceByQuantity() {
        Order order = new Order(1L);
        OrderItem item = new OrderItem(order, 10L, "Mouse", new BigDecimal("25.50"), 3);

        assertThat(item.subtotal()).isEqualByComparingTo("76.50");
    }

    @Test
    @DisplayName("subtotal deve ser zero quando quantity é zero")
    void subtotalShouldBeZeroWhenQuantityIsZero() {
        Order order = new Order(1L);
        OrderItem item = new OrderItem(order, 10L, "Mouse", new BigDecimal("25.50"), 0);

        assertThat(item.subtotal()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("getters devem retornar o snapshot de productId, productName, unitPrice e quantity")
    void gettersShouldReturnSnapshotValues() {
        Order order = new Order(1L);
        OrderItem item = new OrderItem(order, 10L, "Mouse", new BigDecimal("25.50"), 3);

        assertThat(item.getOrder()).isSameAs(order);
        assertThat(item.getProductId()).isEqualTo(10L);
        assertThat(item.getProductName()).isEqualTo("Mouse");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("25.50");
        assertThat(item.getQuantity()).isEqualTo(3);
    }
}
