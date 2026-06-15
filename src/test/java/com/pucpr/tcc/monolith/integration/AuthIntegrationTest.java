package com.pucpr.tcc.monolith.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.tcc.monolith.users.dto.LoginRequest;
import com.pucpr.tcc.monolith.users.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração para o fluxo de autenticação.
 *
 * Usa @SpringBootTest para carregar o contexto completo e
 * MockMvc para simular as requisições HTTP sem subir um servidor real.
 *
 * @Transactional garante rollback após cada teste,
 * mantendo o banco H2 limpo entre execuções.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/register deve criar usuário e retornar JWT")
    void registerShouldReturnJwt() throws Exception {
        var request = new RegisterRequest("Maria Teste", "maria.integration@test.com", "senha123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token", not(emptyString())))
            .andExpect(jsonPath("$.type", is("Bearer")))
            .andExpect(jsonPath("$.email", is("maria.integration@test.com")))
            .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    @Test
    @DisplayName("POST /api/auth/register com e-mail inválido deve retornar 400")
    void registerWithInvalidEmailShouldReturn400() throws Exception {
        var request = new RegisterRequest("Teste", "email-invalido", "senha123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email", not(emptyString())));
    }

    @Test
    @DisplayName("POST /api/auth/register com senha curta deve retornar 400")
    void registerWithShortPasswordShouldReturn400() throws Exception {
        var request = new RegisterRequest("Teste", "teste@test.com", "123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password", not(emptyString())));
    }

    @Test
    @DisplayName("POST /api/auth/login com credenciais válidas deve retornar JWT")
    void loginWithValidCredentialsShouldReturnJwt() throws Exception {
        // Registra o usuário primeiro
        var registerReq = new RegisterRequest("Login Teste", "login.test@test.com", "senha123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)));

        // Faz login
        var loginReq = new LoginRequest("login.test@test.com", "senha123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", not(emptyString())))
            .andExpect(jsonPath("$.email", is("login.test@test.com")));
    }

    @Test
    @DisplayName("POST /api/auth/login com senha errada deve retornar 401")
    void loginWithWrongPasswordShouldReturn401() throws Exception {
        // Registra
        var registerReq = new RegisterRequest("Senha Errada", "senhaerrada@test.com", "correta123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)));

        // Login com senha errada
        var loginReq = new LoginRequest("senhaerrada@test.com", "senha_errada");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/products deve ser acessível sem token (rota pública)")
    void productsShouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/orders sem token deve retornar 403")
    void ordersShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/reports sem token deve retornar 403")
    void reportsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31"))
            .andExpect(status().isForbidden());
    }

    /**
     * Helper: registra um usuário e retorna o token JWT para uso em outros testes.
     */
    protected String registerAndGetToken(String email, String password) throws Exception {
        var request = new RegisterRequest("Usuário Teste", email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
