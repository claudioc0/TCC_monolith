package com.pucpr.tcc.monolith.orders.entity;

import com.pucpr.tcc.monolith.orders.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testa o fluxo de transições de status do pedido.
 * São os testes mais críticos do sistema: garantem que o fluxo
 * PENDENTE → CONFIRMADO → ENVIADO → ENTREGUE seja rigorosamente respeitado.
 */
class OrderStatusTransitionTest {

    private Order newOrder() {
        return new Order(1L); // PENDENTE por padrão
    }

    // ── Transições válidas ────────────────────────────────────

    @Test
    @DisplayName("PENDENTE → CONFIRMADO deve ser permitido")
    void pendente_to_confirmado_allowed() {
        Order order = newOrder();
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMADO));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
    }

    @Test
    @DisplayName("CONFIRMADO → ENVIADO deve ser permitido")
    void confirmado_to_enviado_allowed() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.ENVIADO));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENVIADO);
    }

    @Test
    @DisplayName("ENVIADO → ENTREGUE deve ser permitido")
    void enviado_to_entregue_allowed() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        order.transitionTo(OrderStatus.ENVIADO);
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.ENTREGUE));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENTREGUE);
    }

    @Test
    @DisplayName("PENDENTE → CANCELADO deve ser permitido")
    void pendente_to_cancelado_allowed() {
        Order order = newOrder();
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.CANCELADO));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    @DisplayName("CONFIRMADO → CANCELADO deve ser permitido")
    void confirmado_to_cancelado_allowed() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.CANCELADO));
    }

    // ── Transições inválidas ──────────────────────────────────

    @Test
    @DisplayName("PENDENTE → ENVIADO deve ser bloqueado (pula etapa)")
    void pendente_to_enviado_blocked() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.ENVIADO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("PENDENTE")
                .hasMessageContaining("ENVIADO");
    }

    @Test
    @DisplayName("PENDENTE → ENTREGUE deve ser bloqueado (pula etapa)")
    void pendente_to_entregue_blocked() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.ENTREGUE))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("ENVIADO → CANCELADO deve ser bloqueado")
    void enviado_to_cancelado_blocked() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        order.transitionTo(OrderStatus.ENVIADO);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELADO))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("ENTREGUE → qualquer status deve ser bloqueado (estado terminal)")
    void entregue_is_terminal() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        order.transitionTo(OrderStatus.ENVIADO);
        order.transitionTo(OrderStatus.ENTREGUE);

        for (OrderStatus s : OrderStatus.values()) {
            assertThatThrownBy(() -> order.transitionTo(s))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    @DisplayName("CANCELADO → qualquer status deve ser bloqueado (estado terminal)")
    void cancelado_is_terminal() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CANCELADO);

        for (OrderStatus s : OrderStatus.values()) {
            assertThatThrownBy(() -> order.transitionTo(s))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    // ── Cálculo de total ──────────────────────────────────────

    @Test
    @DisplayName("totalAmount deve ser zero para pedido sem itens")
    void totalAmountShouldBeZeroForEmptyOrder() {
        Order order = newOrder();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0.00");
    }
}
