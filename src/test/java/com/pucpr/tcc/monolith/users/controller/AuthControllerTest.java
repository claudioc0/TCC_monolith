package com.pucpr.tcc.monolith.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.tcc.monolith.security.JwtAuthenticationFilter;
import com.pucpr.tcc.monolith.security.JwtService;
import com.pucpr.tcc.monolith.security.SecurityConfig;
import com.pucpr.tcc.monolith.users.dto.AuthResponse;
import com.pucpr.tcc.monolith.users.dto.LoginRequest;
import com.pucpr.tcc.monolith.users.dto.RegisterRequest;
import com.pucpr.tcc.monolith.users.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test de AuthController: as duas únicas rotas públicas do sistema
 * (permitAll em SecurityConfig). Exercita a cadeia real de segurança para
 * confirmar que register/login realmente não exigem token, e a integração
 * com GlobalExceptionHandler para e-mail duplicado, validação e credenciais
 * inválidas.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private AuthenticationProvider authenticationProvider;

    @Test
    @DisplayName("POST /api/auth/register sem token deve criar e retornar 201 com o token")
    void registerShouldReturn201WithToken() throws Exception {
        var request = new RegisterRequest("João", "joao@email.com", "senha123");
        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(AuthResponse.of("jwt.token", 1L, "João", "joao@email.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/auth/register com e-mail já cadastrado deve retornar 400")
    void registerWithDuplicateEmailShouldReturn400() throws Exception {
        var request = new RegisterRequest("João", "joao@email.com", "senha123");
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("E-mail já cadastrado: joao@email.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado: joao@email.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register com senha curta deve retornar 400 com erro de validação")
    void registerWithShortPasswordShouldReturn400() throws Exception {
        var request = new RegisterRequest("João", "joao@email.com", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login com credenciais válidas deve retornar 200 com o token")
    void loginShouldReturn200WithToken() throws Exception {
        var request = new LoginRequest("joao@email.com", "senha123");
        when(userService.login(any(LoginRequest.class)))
                .thenReturn(AuthResponse.of("jwt.token", 1L, "João", "joao@email.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token"));
    }

    @Test
    @DisplayName("POST /api/auth/login com credenciais inválidas deve retornar 401")
    void loginWithInvalidCredentialsShouldReturn401() throws Exception {
        var request = new LoginRequest("joao@email.com", "senha_errada");
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("POST /api/auth/login com e-mail em formato inválido deve retornar 400")
    void loginWithMalformedEmailShouldReturn400() throws Exception {
        var request = new LoginRequest("nao-e-email", "senha123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }
}
