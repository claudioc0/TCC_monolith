package com.pucpr.tcc.ecommerce.order.application;

import com.pucpr.tcc.ecommerce.customer.application.CustomerService;
import com.pucpr.tcc.ecommerce.order.domain.*;
import com.pucpr.tcc.ecommerce.product.application.ProductService;
import com.pucpr.tcc.ecommerce.product.domain.Product;
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

    @Mock OrderRepository orderRepository;
    @Mock CustomerService customerService;
    @Mock ProductService productService;

    @InjectMocks OrderService orderService;

    @Test
    @DisplayName("Checkout lança exceção se cliente inativo")
    void checkoutThrowsForInactiveCustomer() {
        when(customerService.isActive(1L)).thenReturn(false);

        CheckoutRequest req = new CheckoutRequest(1L,
                List.of(new CheckoutRequest.CheckoutItemRequest(10L, 2)));

        assertThatThrownBy(() -> orderService.checkout(req))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("inativo");

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("Checkout lança exceção se produto sem estoque")
    void checkoutThrowsWhenOutOfStock() {
        when(customerService.isActive(1L)).thenReturn(true);
        when(productService.checkStock(10L, 5)).thenReturn(false);

        CheckoutRequest req = new CheckoutRequest(1L,
                List.of(new CheckoutRequest.CheckoutItemRequest(10L, 5)));

        assertThatThrownBy(() -> orderService.checkout(req))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    @DisplayName("Checkout bem-sucedido cria pedido e baixa estoque")
    void checkoutSuccessCreatesOrderAndDecreasesStock() {
        when(customerService.isActive(1L)).thenReturn(true);
        when(productService.checkStock(10L, 2)).thenReturn(true);

        Product product = new Product("Produto", new BigDecimal("200.00"), 10);
        when(productService.findById(10L)).thenReturn(product);

        Order mockOrder = new Order(1L, List.of(new OrderItem(10L, 2, new BigDecimal("200.00"))));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        Order result = orderService.checkout(
                new CheckoutRequest(1L, List.of(new CheckoutRequest.CheckoutItemRequest(10L, 2))));

        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
        verify(productService).decreaseStock(10L, 2);
    }

    @Test
    @DisplayName("findById lança exceção para pedido inexistente")
    void findByIdThrowsForUnknown() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("cancel delega ao método cancel da entidade")
    void cancelDelegatesToEntity() {
        Order order = new Order(1L, List.of(new OrderItem(1L, 1, new BigDecimal("50.00"))));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.cancel(1L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("pay delega ao método pay da entidade")
    void payDelegatesToEntity() {
        Order order = new Order(1L, List.of(new OrderItem(1L, 1, new BigDecimal("50.00"))));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.pay(1L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
