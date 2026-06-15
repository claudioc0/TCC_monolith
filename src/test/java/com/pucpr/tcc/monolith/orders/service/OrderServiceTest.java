package com.pucpr.tcc.monolith.orders.service;

import com.pucpr.tcc.monolith.orders.dto.OrderRequest;
import com.pucpr.tcc.monolith.orders.dto.OrderRequest.OrderItemRequest;
import com.pucpr.tcc.monolith.orders.dto.UpdateStatusRequest;
import com.pucpr.tcc.monolith.orders.entity.Order;
import com.pucpr.tcc.monolith.orders.entity.OrderStatus;
import com.pucpr.tcc.monolith.orders.exception.InvalidStatusTransitionException;
import com.pucpr.tcc.monolith.orders.exception.OrderNotFoundException;
import com.pucpr.tcc.monolith.orders.repository.OrderRepository;
import com.pucpr.tcc.monolith.products.entity.Product;
import com.pucpr.tcc.monolith.products.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductService productService;
    @InjectMocks private OrderService orderService;

    @Test
    @DisplayName("create deve criar pedido descontando estoque")
    void createShouldCreateOrderAndDecreaseStock() {
        Product product = new Product("Mouse", "Gamer", new BigDecimal("150.00"), 5);
        when(productService.getProductOrThrow(1L)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));
        var response = orderService.create(1L, request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDENTE);
        assertThat(response.totalAmount()).isEqualByComparingTo("300.00");
        verify(productService).decreaseStock(1L, 2);
    }

    @Test
    @DisplayName("updateStatus deve seguir fluxo PENDENTE → CONFIRMADO")
    void updateStatusShouldTransitionCorrectly() {
        Order order = new Order(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = orderService.updateStatus(1L, new UpdateStatusRequest(OrderStatus.CONFIRMADO));

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMADO);
    }

    @Test
    @DisplayName("updateStatus deve lançar exceção para transição inválida ENTREGUE → PENDENTE")
    void updateStatusShouldThrowForInvalidTransition() {
        Order order = new Order(1L);
        // Avança para ENTREGUE manualmente
        order.transitionTo(OrderStatus.CONFIRMADO);
        order.transitionTo(OrderStatus.ENVIADO);
        order.transitionTo(OrderStatus.ENTREGUE);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateStatus(1L, new UpdateStatusRequest(OrderStatus.PENDENTE)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("cancel deve cancelar pedido PENDENTE e devolver estoque")
    void cancelShouldCancelAndRestoreStock() {
        Order order = new Order(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = orderService.cancel(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    @DisplayName("findById deve lançar OrderNotFoundException para id inexistente")
    void findByIdShouldThrowForUnknownId() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
