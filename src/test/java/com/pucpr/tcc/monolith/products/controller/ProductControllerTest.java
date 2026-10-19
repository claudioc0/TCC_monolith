package com.pucpr.tcc.monolith.products.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.tcc.monolith.products.dto.ProductRequest;
import com.pucpr.tcc.monolith.products.dto.ProductResponse;
import com.pucpr.tcc.monolith.products.exception.ProductNotFoundException;
import com.pucpr.tcc.monolith.products.service.ProductService;
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
 * Slice test de ProductController: sobe só a camada web (o {@link ProductService}
 * é mockado) mas importa a cadeia de segurança real
 * ({@link SecurityConfig} + {@link JwtAuthenticationFilter}) para verificar,
 * com tokens JWT de verdade, que as rotas de leitura são públicas e as de
 * escrita exigem a role ADMIN — exatamente a combinação que faltava no
 * monólito e que concentra boa parte do gap de cobertura em relação à
 * versão microsserviços.
 */
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockBean private ProductService productService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private AuthenticationProvider authenticationProvider;

    private String adminAuthHeader() {
        return SecurityTestSupport.bearerTokenFor(jwtService, userDetailsService, SecurityTestSupport.adminUser(1L));
    }

    private String customerAuthHeader() {
        return SecurityTestSupport.bearerTokenFor(jwtService, userDetailsService, SecurityTestSupport.customerUser(2L));
    }

    // ── Leitura (pública, sem autenticação) ───────────────────

    @Test
    @DisplayName("GET /api/products deve ser público e retornar a lista do service")
    void findAllShouldBePublicAndReturnServiceList() throws Exception {
        var response = new ProductResponse(1L, "Mouse", "Sem fio", new BigDecimal("99.90"), 10,
                LocalDateTime.now(), LocalDateTime.now());
        when(productService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mouse"))
                .andExpect(jsonPath("$[0].price").value(99.90));
    }

    @Test
    @DisplayName("GET /api/products/{id} deve retornar 404 quando o produto não existe")
    void findByIdShouldReturn404WhenNotFound() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/products/search deve repassar o parâmetro name ao service")
    void searchByNameShouldDelegateToService() throws Exception {
        when(productService.searchByName("mou")).thenReturn(List.of());

        mockMvc.perform(get("/api/products/search").param("name", "mou"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(productService).searchByName("mou");
    }

    // ── Escrita (somente ADMIN) ────────────────────────────────

    @Test
    @DisplayName("POST /api/products com ADMIN deve criar e retornar 201")
    void createAsAdminShouldReturn201() throws Exception {
        var request = new ProductRequest("Teclado", "Mecânico", new BigDecimal("350.00"), 5);
        var response = new ProductResponse(1L, "Teclado", "Mecânico", new BigDecimal("350.00"), 5,
                LocalDateTime.now(), LocalDateTime.now());
        when(productService.create(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Teclado"));
    }

    @Test
    @DisplayName("POST /api/products com CUSTOMER deve retornar 403")
    void createAsCustomerShouldReturn403() throws Exception {
        var request = new ProductRequest("Teclado", "Mecânico", new BigDecimal("350.00"), 5);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", customerAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products sem token deve retornar 401/403 (não autenticado)")
    void createWithoutTokenShouldBeRejected() throws Exception {
        var request = new ProductRequest("Teclado", "Mecânico", new BigDecimal("350.00"), 5);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/products com preço negativo deve retornar 400 com o erro de validação")
    void createWithInvalidPriceShouldReturn400() throws Exception {
        var request = new ProductRequest("Produto", "Desc", new BigDecimal("-1.00"), 5);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    @DisplayName("PUT /api/products/{id} com ADMIN deve atualizar e retornar 200")
    void updateAsAdminShouldReturn200() throws Exception {
        var request = new ProductRequest("Teclado Pro", "Mecânico RGB", new BigDecimal("450.00"), 3);
        var response = new ProductResponse(1L, "Teclado Pro", "Mecânico RGB", new BigDecimal("450.00"), 3,
                LocalDateTime.now(), LocalDateTime.now());
        when(productService.update(eq(1L), any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/products/1")
                        .header("Authorization", adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Teclado Pro"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} com CUSTOMER deve retornar 403")
    void updateAsCustomerShouldReturn403() throws Exception {
        var request = new ProductRequest("Teclado Pro", "Mecânico RGB", new BigDecimal("450.00"), 3);

        mockMvc.perform(put("/api/products/1")
                        .header("Authorization", customerAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} com ADMIN deve remover e retornar 204")
    void deleteAsAdminShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/products/1").header("Authorization", adminAuthHeader()))
                .andExpect(status().isNoContent());

        verify(productService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} com CUSTOMER deve retornar 403 e não chamar o service")
    void deleteAsCustomerShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/products/1").header("Authorization", customerAuthHeader()))
                .andExpect(status().isForbidden());

        verify(productService, org.mockito.Mockito.never()).delete(any());
    }
}
