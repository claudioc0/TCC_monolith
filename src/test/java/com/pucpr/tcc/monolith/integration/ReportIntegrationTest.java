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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração para o relatório de vendas por período.
 *
 * O endpoint é ADMIN-only a nível de classe ({@code @PreAuthorize} no ReportController),
 * então a maior parte da superfície de teste é: controle de acesso + agregação correta
 * dos totais a partir de pedidos com status diferentes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String customerToken;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAsSeededAdmin();
        customerToken = registerAndGetToken("report.customer@test.com", "senha123");
    }

    @Test
    @DisplayName("GET /api/reports/sales com CUSTOMER deve retornar 403")
    void salesReportAsCustomerShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                .header("Authorization", "Bearer " + customerToken)
                .param("startDate", today.toString())
                .param("endDate", today.toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/reports/sales com ADMIN deve agregar pedidos ENTREGUE e CANCELADO corretamente")
    void salesReportAsAdminShouldAggregateOrderTotals() throws Exception {
        long productId = createProduct("Impressora", "300.00", 10);

        // Pedido 1: avança até ENTREGUE
        long deliveredOrderId = createOrderAs(customerToken, productId, 1);
        advanceStatus(deliveredOrderId, "CONFIRMADO");
        advanceStatus(deliveredOrderId, "ENVIADO");
        advanceStatus(deliveredOrderId, "ENTREGUE");

        // Pedido 2: cancelado ainda PENDENTE
        long canceledOrderId = createOrderAs(customerToken, productId, 1);
        mockMvc.perform(patch("/api/orders/" + canceledOrderId + "/cancel")
                .header("Authorization", "Bearer " + customerToken));

        mockMvc.perform(get("/api/reports/sales")
                .header("Authorization", "Bearer " + adminToken)
                .param("startDate", today.toString())
                .param("endDate", today.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders", is(2)))
            .andExpect(jsonPath("$.deliveredOrders", is(1)))
            .andExpect(jsonPath("$.canceledOrders", is(1)))
            .andExpect(jsonPath("$.totalRevenue", is(300.00)))
            .andExpect(jsonPath("$.orders", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/reports/sales para período sem pedidos deve retornar totais zerados")
    void salesReportForPeriodWithNoOrdersShouldReturnZeroTotals() throws Exception {
        LocalDate farPast = LocalDate.of(2000, 1, 1);

        mockMvc.perform(get("/api/reports/sales")
                .header("Authorization", "Bearer " + adminToken)
                .param("startDate", farPast.toString())
                .param("endDate", farPast.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrders", is(0)))
            .andExpect(jsonPath("$.deliveredOrders", is(0)))
            .andExpect(jsonPath("$.canceledOrders", is(0)))
            .andExpect(jsonPath("$.totalRevenue", is(0)))
            .andExpect(jsonPath("$.orders", hasSize(0)));
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

    private void advanceStatus(long orderId, String newStatus) throws Exception {
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"" + newStatus + "\"}"))
            .andExpect(status().isOk());
    }
}
