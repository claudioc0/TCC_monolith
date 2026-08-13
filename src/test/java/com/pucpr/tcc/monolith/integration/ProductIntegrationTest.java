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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração para o módulo de Produtos.
 * Verifica criação, busca, validação e controle de acesso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Registra um ADMIN — necessário pois data.sql pode não rodar no contexto de teste
        // Em produção, o admin seria promovido manualmente ou via script de inicialização
        adminToken    = registerAndGetToken("admin.int@test.com", "admin123");
        customerToken = registerAndGetToken("customer.int@test.com", "customer123");
    }

    @Test
    @DisplayName("GET /api/products deve retornar lista sem autenticação")
    void getAllProductsShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("POST /api/products com ADMIN real deve criar produto e retornar todos os campos")
    void createProductAsRealAdminShouldSucceedAndReturnAllFields() throws Exception {
        String realAdminToken = loginAsSeededAdmin();
        var request = new ProductRequest("Cadeira Gamer", "Ergonômica", new BigDecimal("899.90"), 7);

        MvcResult result = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + realAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.name", is("Cadeira Gamer")))
            .andExpect(jsonPath("$.description", is("Ergonômica")))
            .andExpect(jsonPath("$.price", is(899.90)))
            .andExpect(jsonPath("$.stockQuantity", is(7)))
            .andReturn();

        // Via JsonNode: not(emptyString()) nao rejeita um valor JSON null.
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(json.get("createdAt").isNull()).isFalse();
        org.assertj.core.api.Assertions.assertThat(json.get("updatedAt").isNull()).isFalse();
    }

    @Test
    @DisplayName("POST /api/products com CUSTOMER deve retornar 403")
    void createProductAsCustomerShouldBeForbidden() throws Exception {
        var request = new ProductRequest("Produto", "Desc", new BigDecimal("100.00"), 10);

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/products/{id} deve retornar 404 para produto inexistente")
    void getByIdShouldReturn404ForUnknownProduct() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", containsString("não encontrado")));
    }

    @Test
    @DisplayName("POST /api/products com preço inválido deve retornar 400")
    void createProductWithInvalidPriceShouldReturn400() throws Exception {
        var request = new ProductRequest("Produto", "Desc", new BigDecimal("-10.00"), 5);

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.price", not(emptyString())));
    }

    // ── Helper ────────────────────────────────────────────────

    private String loginAsSeededAdmin() throws Exception {
        var request = new LoginRequest("admin@tcc.com", "admin123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String registerAndGetToken(String email, String password) throws Exception {
        var request = new RegisterRequest("Teste", email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
