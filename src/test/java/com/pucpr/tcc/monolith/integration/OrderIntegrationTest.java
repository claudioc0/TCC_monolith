package com.pucpr.tcc.monolith.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.tcc.monolith.products.dto.ProductRequest;
import com.pucpr.tcc.monolith.users.dto.LoginRequest;
import com.pucpr.tcc.monolith.users.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração para o módulo de Pedidos.
 *
 * Cobre o fluxo completo via MockMvc: criação com desconto de estoque,
 * consultas, transições de status restritas a ADMIN e cancelamento.
 *
 * O token "admin" usa o usuário semeado por {@link com.pucpr.tcc.monolith.security.DatabaseSeeder}
 * (admin@tcc.com) — é o único jeito de obter uma role ADMIN real neste sistema,
 * já que POST /api/auth/register sempre cria usuários CUSTOMER.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAsSeededAdmin();
        customerToken = registerAndGetToken("order.customer@test.com", "senha123");
    }

    @Test
    @DisplayName("POST /api/orders deve criar pedido descontando estoque e retornar 201")
    void createOrderShouldSucceedAndDecreaseStock() throws Exception {
        long productId = createProduct("Teclado Mecânico", "10.00", 5);

        String body = """
            {"items":[{"productId": %d, "quantity": 2}]}
            """.formatted(productId);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status", is("PENDENTE")))
            .andExpect(jsonPath("$.totalAmount", is(20.00)))
            .andExpect(jsonPath("$.createdAt", not(emptyString())))
            .andExpect(jsonPath("$.updatedAt", not(emptyString())))
            .andExpect(jsonPath("$.items[0].id", notNullValue()))
            .andExpect(jsonPath("$.items[0].quantity", is(2)))
            .andExpect(jsonPath("$.items[0].productName", is("Teclado Mecânico")))
            .andExpect(jsonPath("$.items[0].unitPrice", is(10.00)));

        // estoque deve ter sido descontado: 5 - 2 = 3
        mockMvc.perform(get("/api/products/" + productId))
            .andExpect(jsonPath("$.stockQuantity", is(3)));
    }

    @Test
    @DisplayName("POST /api/orders para produto inexistente deve retornar 404")
    void createOrderWithUnknownProductShouldReturn404() throws Exception {
        String body = """
            {"items":[{"productId": 999999, "quantity": 1}]}
            """;

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/orders com estoque insuficiente deve retornar 422")
    void createOrderWithInsufficientStockShouldReturn422() throws Exception {
        long productId = createProduct("Monitor", "500.00", 1);

        String body = """
            {"items":[{"productId": %d, "quantity": 5}]}
            """.formatted(productId);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message", containsString("Estoque insuficiente")));
    }

    @Test
    @DisplayName("POST /api/orders sem itens deve retornar 400")
    void createOrderWithNoItemsShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.items", not(emptyString())));
    }

    @Test
    @DisplayName("GET /api/orders/my-orders deve listar somente os pedidos do usuário autenticado")
    void myOrdersShouldReturnOnlyOwnOrders() throws Exception {
        long productId = createProduct("Mouse", "50.00", 10);
        createOrderAs(customerToken, productId, 1);

        mockMvc.perform(get("/api/orders/my-orders")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status", is("PENDENTE")));
    }

    @Test
    @DisplayName("GET /api/orders/{id} deve retornar 404 para pedido inexistente")
    void findByIdShouldReturn404ForUnknownOrder() throws Exception {
        mockMvc.perform(get("/api/orders/999999")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", containsString("não encontrado")));
    }

    @Test
    @DisplayName("GET /api/orders com CUSTOMER deve retornar 403")
    void findAllAsCustomerShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/orders com ADMIN deve retornar 200 com a lista completa")
    void findAllAsAdminShouldReturnOk() throws Exception {
        long productId = createProduct("Cadeira", "300.00", 3);
        createOrderAs(customerToken, productId, 1);

        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status com CUSTOMER deve retornar 403")
    void updateStatusAsCustomerShouldBeForbidden() throws Exception {
        long productId = createProduct("Fone", "80.00", 5);
        long orderId = createOrderAs(customerToken, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"CONFIRMADO\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status com ADMIN e transição válida deve retornar 200")
    void updateStatusAsAdminWithValidTransitionShouldSucceed() throws Exception {
        long productId = createProduct("Webcam", "150.00", 5);
        long orderId = createOrderAs(customerToken, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"CONFIRMADO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CONFIRMADO")));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status com transição inválida deve retornar 422")
    void updateStatusWithInvalidTransitionShouldReturn422() throws Exception {
        long productId = createProduct("Headset", "120.00", 5);
        long orderId = createOrderAs(customerToken, productId, 1);

        // PENDENTE -> ENVIADO pula etapa, deve ser bloqueado
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"ENVIADO\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message", containsString("PENDENTE")));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel deve cancelar pedido PENDENTE e devolver estoque")
    void cancelShouldCancelOrderAndRestoreStock() throws Exception {
        long productId = createProduct("SSD", "400.00", 4);
        long orderId = createOrderAs(customerToken, productId, 2);

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CANCELADO")));

        // estoque devolvido: 4 - 2 (compra) + 2 (cancelamento) = 4
        mockMvc.perform(get("/api/products/" + productId))
            .andExpect(jsonPath("$.stockQuantity", is(4)));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel em pedido já cancelado deve retornar 422")
    void cancelAlreadyCanceledOrderShouldReturn422() throws Exception {
        long productId = createProduct("Placa de Vídeo", "2000.00", 2);
        long orderId = createOrderAs(customerToken, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + customerToken));

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isUnprocessableEntity());
    }

    // ── Helpers ───────────────────────────────────────────────

    private String registerAndGetToken(String email, String password) throws Exception {
        var request = new RegisterRequest("Cliente Teste", email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String loginAsSeededAdmin() throws Exception {
        var request = new LoginRequest("admin@tcc.com", "admin123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createProduct(String name, String price, int stock) throws Exception {
        var request = new ProductRequest(name, "desc", new BigDecimal(price), stock);
        MvcResult result = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createOrderAs(String token, long productId, int quantity) throws Exception {
        String body = """
            {"items":[{"productId": %d, "quantity": %d}]}
            """.formatted(productId, quantity);
        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
