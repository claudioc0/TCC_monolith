package com.pucpr.tcc.monolith.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.tcc.monolith.orders.dto.OrderRequest;
import com.pucpr.tcc.monolith.orders.dto.OrderRequest.OrderItemRequest;
import com.pucpr.tcc.monolith.orders.dto.OrderResponse;
import com.pucpr.tcc.monolith.orders.dto.UpdateStatusRequest;
import com.pucpr.tcc.monolith.orders.entity.OrderStatus;
import com.pucpr.tcc.monolith.orders.exception.OrderNotFoundException;
import com.pucpr.tcc.monolith.orders.service.OrderService;
import com.pucpr.tcc.monolith.security.JwtAuthenticationFilter;
import com.pucpr.tcc.monolith.security.JwtService;
import com.pucpr.tcc.monolith.security.SecurityConfig;
import com.pucpr.tcc.monolith.testsupport.SecurityTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test de OrderController: exercita a cadeia de segurança real
 * ({@link SecurityConfig} + {@link JwtAuthenticationFilter}) com o
 * {@link OrderService} mockado.
 *
 * O teste mais importante desta classe é
 * {@link #createShouldUseAuthenticatedUserIdNotBodyValue()}: garante que o
 * userId usado para criar o pedido vem do principal autenticado (JWT), nunca
 * de um campo que o cliente poderia manipular no corpo da requisição — essa
 * é justamente a razão de {@code OrderRequest} não ter um campo userId.
 */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockBean private OrderService orderService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private AuthenticationProvider authenticationProvider;

    private String adminAuthHeader() {
        return SecurityTestSupport.bearerTokenFor(jwtService, userDetailsService, SecurityTestSupport.adminUser(1L));
    }

    private String customerAuthHeader(long id) {
        return SecurityTestSupport.bearerTokenFor(jwtService, userDetailsService, SecurityTestSupport.customerUser(id));
    }

    private OrderResponse sampleResponse(long userId) {
        return new OrderResponse(1L, userId, List.of(), new BigDecimal("300.00"),
                OrderStatus.PENDENTE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/orders deve usar o id do usuário autenticado, não um valor do corpo")
    void createShouldUseAuthenticatedUserIdNotBodyValue() throws Exception {
        long authenticatedUserId = 42L;
        when(orderService.create(eq(authenticatedUserId), any(OrderRequest.class)))
                .thenReturn(sampleResponse(authenticatedUserId));
        var request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", customerAuthHeader(authenticatedUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(authenticatedUserId));

        verify(orderService).create(eq(authenticatedUserId), any(OrderRequest.class));
    }

    @Test
    @DisplayName("POST /api/orders sem autenticação deve ser rejeitado")
    void createWithoutAuthenticationShouldBeRejected() throws Exception {
        var request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/orders com lista de itens vazia deve retornar 400")
    void createWithEmptyItemsShouldReturn400() throws Exception {
        var request = new OrderRequest(List.of());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", customerAuthHeader(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    @DisplayName("GET /api/orders/my-orders deve usar o id do usuário autenticado")
    void myOrdersShouldUseAuthenticatedUserId() throws Exception {
        long authenticatedUserId = 7L;
        when(orderService.findByUser(authenticatedUserId)).thenReturn(List.of(sampleResponse(authenticatedUserId)));

        mockMvc.perform(get("/api/orders/my-orders").header("Authorization", customerAuthHeader(authenticatedUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(authenticatedUserId));

        verify(orderService).findByUser(authenticatedUserId);
    }

    @Test
    @DisplayName("GET /api/orders/{id} deve retornar 404 para pedido inexistente")
    void findByIdShouldReturn404WhenNotFound() throws Exception {
        when(orderService.findById(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/orders/99").header("Authorization", customerAuthHeader(1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/orders/{id} deve retornar o pedido do service quando encontrado")
    void findByIdShouldReturnOrderWhenFound() throws Exception {
        when(orderService.findById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/orders/1").header("Authorization", customerAuthHeader(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("GET /api/orders (findAll) com ADMIN deve retornar 200")
    void findAllAsAdminShouldReturn200() throws Exception {
        when(orderService.findAll()).thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        mockMvc.perform(get("/api/orders").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/orders (findAll) com CUSTOMER deve retornar 403")
    void findAllAsCustomerShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", customerAuthHeader(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status com ADMIN deve atualizar o status")
    void updateStatusAsAdminShouldSucceed() throws Exception {
        var request = new UpdateStatusRequest(OrderStatus.CONFIRMADO);
        var response = new OrderResponse(1L, 1L, List.of(), new BigDecimal("300.00"),
                OrderStatus.CONFIRMADO, LocalDateTime.now(), LocalDateTime.now());
        when(orderService.updateStatus(eq(1L), any(UpdateStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/orders/1/status")
                        .header("Authorization", adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status com CUSTOMER deve retornar 403 e não chamar o service")
    void updateStatusAsCustomerShouldReturn403() throws Exception {
        var request = new UpdateStatusRequest(OrderStatus.CONFIRMADO);

        mockMvc.perform(patch("/api/orders/1/status")
                        .header("Authorization", customerAuthHeader(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(orderService, org.mockito.Mockito.never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel com CUSTOMER autenticado deve ser permitido")
    void cancelAsAuthenticatedCustomerShouldSucceed() throws Exception {
        when(orderService.cancel(1L)).thenReturn(
                new OrderResponse(1L, 1L, List.of(), new BigDecimal("300.00"),
                        OrderStatus.CANCELADO, LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(patch("/api/orders/1/cancel").header("Authorization", customerAuthHeader(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }
}
