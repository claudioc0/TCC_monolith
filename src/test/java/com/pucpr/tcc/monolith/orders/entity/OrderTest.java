package com.pucpr.tcc.monolith.orders.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa o comportamento de domínio de Order que não é transição de status
 * (isso já é coberto por {@link OrderStatusTransitionTest}): adição de itens
 * e o recálculo automático de totalAmount, e a imutabilidade da lista
 * exposta por getItems().
 */
class OrderTest {

    @Test
    @DisplayName("addItem deve recalcular totalAmount como soma dos subtotais")
    void addItemShouldRecalculateTotal() {
        Order order = new Order(1L);
        order.addItem(new OrderItem(order, 10L, "Mouse", new BigDecimal("50.00"), 2));
        order.addItem(new OrderItem(order, 20L, "Teclado", new BigDecimal("150.00"), 1));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("addItem deve adicionar o item à lista de itens do pedido")
    void addItemShouldAppendToItemsList() {
        Order order = new Order(1L);
        OrderItem item = new OrderItem(order, 10L, "Mouse", new BigDecimal("50.00"), 2);
        order.addItem(item);

        assertThat(order.getItems()).containsExactly(item);
    }

    @Test
    @DisplayName("getItems deve retornar lista não modificável")
    void getItemsShouldBeUnmodifiable() {
        Order order = new Order(1L);
        order.addItem(new OrderItem(order, 10L, "Mouse", new BigDecimal("50.00"), 1));

        assertThatThrownBy(() -> order.getItems().add(
                new OrderItem(order, 20L, "Teclado", new BigDecimal("1.00"), 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getUserId deve retornar o id passado no construtor")
    void getUserIdShouldReturnConstructorValue() {
        Order order = new Order(42L);
        assertThat(order.getUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("pedido novo deve iniciar com status PENDENTE")
    void newOrderShouldStartAsPendente() {
        Order order = new Order(1L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDENTE);
    }
}
