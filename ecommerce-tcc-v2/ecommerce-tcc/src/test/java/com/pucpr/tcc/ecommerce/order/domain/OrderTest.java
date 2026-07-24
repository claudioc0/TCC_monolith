package com.pucpr.tcc.ecommerce.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    private OrderItem item(double price, int qty) {
        return new OrderItem(1L, qty, new BigDecimal(String.valueOf(price)));
    }

    @Test
    @DisplayName("Deve criar pedido com status PENDING")
    void shouldCreatePendingOrder() {
        Order o = new Order(1L, List.of(item(100.0, 2)));
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(o.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Não deve criar pedido sem itens")
    void shouldRejectEmptyItems() {
        assertThatThrownBy(() -> new Order(1L, List.of()))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Não deve criar pedido com itens null")
    void shouldRejectNullItems() {
        assertThatThrownBy(() -> new Order(1L, null))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Total sem desconto: subtotal abaixo de R$500")
    void totalWithoutDiscountBelow500() {
        // 3 × R$100 = R$300 → sem desconto
        Order o = new Order(1L, List.of(item(100.0, 3)));
        assertThat(o.getTotalAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("Total sem desconto: exatamente R$500 (limiar exclusivo)")
    void totalWithoutDiscountAtExact500() {
        // 5 × R$100 = R$500 → sem desconto (regra é > 500)
        Order o = new Order(1L, List.of(item(100.0, 5)));
        assertThat(o.getTotalAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Total COM desconto 10%: subtotal acima de R$500")
    void totalWithDiscountAbove500() {
        // 6 × R$100 = R$600 → 10% desc → R$540
        Order o = new Order(1L, List.of(item(100.0, 6)));
        assertThat(o.getTotalAmount()).isEqualByComparingTo("540.00");
    }

    @Test
    @DisplayName("Desconto com múltiplos itens somando acima de R$500")
    void discountWithMultipleItems() {
        // R$300 + R$300 = R$600 → R$540
        Order o = new Order(1L, List.of(
                new OrderItem(1L, 3, new BigDecimal("100.00")),
                new OrderItem(2L, 3, new BigDecimal("100.00"))));
        assertThat(o.getTotalAmount()).isEqualByComparingTo("540.00");
    }

    @Test
    @DisplayName("Deve cancelar pedido PENDING")
    void shouldCancelPending() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.cancel();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("Não deve cancelar pedido CONFIRMED")
    void shouldNotCancelConfirmed() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.confirm();
        assertThatThrownBy(o::cancel).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Não deve cancelar pedido já CANCELED")
    void shouldNotCancelAlreadyCanceled() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.cancel();
        assertThatThrownBy(o::cancel).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Deve confirmar pedido PENDING")
    void shouldConfirmPending() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.confirm();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Não deve confirmar pedido CANCELED")
    void shouldNotConfirmCanceled() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.cancel();
        assertThatThrownBy(o::confirm).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Não deve confirmar pedido já CONFIRMED")
    void shouldNotConfirmAlreadyConfirmed() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.confirm();
        assertThatThrownBy(o::confirm).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Deve enviar pedido CONFIRMED")
    void shouldShipConfirmed() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.confirm();
        o.ship();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("Não deve enviar pedido PENDING")
    void shouldNotShipPending() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        assertThatThrownBy(o::ship).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Deve entregar pedido SHIPPED")
    void shouldDeliverShipped() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.confirm();
        o.ship();
        o.deliver();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("Não deve entregar pedido CONFIRMED (ainda não enviado)")
    void shouldNotDeliverConfirmed() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.confirm();
        assertThatThrownBy(o::deliver).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("Não deve entregar pedido já DELIVERED")
    void shouldNotDeliverAlreadyDelivered() {
        Order o = new Order(1L, List.of(item(100.0, 1)));
        o.confirm();
        o.ship();
        o.deliver();
        assertThatThrownBy(o::deliver).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("OrderItem deve calcular subtotal")
    void orderItemSubtotal() {
        OrderItem oi = new OrderItem(1L, 3, new BigDecimal("150.00"));
        assertThat(oi.subtotal()).isEqualByComparingTo("450.00");
    }
}
