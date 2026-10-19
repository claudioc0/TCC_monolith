package com.pucpr.tcc.monolith.users.controller;

import com.pucpr.tcc.monolith.security.JwtAuthenticationFilter;
import com.pucpr.tcc.monolith.security.JwtService;
import com.pucpr.tcc.monolith.security.SecurityConfig;
import com.pucpr.tcc.monolith.testsupport.SecurityTestSupport;
import com.pucpr.tcc.monolith.users.dto.UserResponse;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import com.pucpr.tcc.monolith.users.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test de UserController: exercita a cadeia de segurança real
 * ({@link SecurityConfig} + {@link JwtAuthenticationFilter}) com o
 * {@link UserService} mockado.
 *
 * {@link #meShouldReturnAuthenticatedUserProfileNotArbitraryId()} é o teste
 * crítico: GET /me não recebe nenhum id na URL — o perfil retornado tem que
 * ser necessariamente o do usuário do token, nunca de outro id possível de
 * adivinhar.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockBean private UserService userService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private AuthenticationProvider authenticationProvider;

    private String adminAuthHeader() {
        return SecurityTestSupport.bearerTokenFor(jwtService, userDetailsService, SecurityTestSupport.adminUser(1L));
    }

    private String customerAuthHeader(long id) {
        return SecurityTestSupport.bearerTokenFor(jwtService, userDetailsService, SecurityTestSupport.customerUser(id));
    }

    @Test
    @DisplayName("GET /api/users/me deve retornar o perfil do usuário autenticado, extraído do token")
    void meShouldReturnAuthenticatedUserProfileNotArbitraryId() throws Exception {
        long authenticatedUserId = 99L;

        mockMvc.perform(get("/api/users/me").header("Authorization", customerAuthHeader(authenticatedUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente.slice@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(userService, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("GET /api/users/me sem token deve ser rejeitado")
    void meWithoutTokenShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/users com ADMIN deve retornar a lista do service")
    void findAllAsAdminShouldReturnServiceList() throws Exception {
        var response = new UserResponse(1L, "Admin", "admin@test.com", "ADMIN", LocalDateTime.now());
        when(userService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@test.com"));
    }

    @Test
    @DisplayName("GET /api/users com CUSTOMER deve retornar 403")
    void findAllAsCustomerShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", customerAuthHeader(1L)))
                .andExpect(status().isForbidden());

        verify(userService, never()).findAll();
    }

    @Test
    @DisplayName("GET /api/users/{id} com ADMIN deve retornar o usuário")
    void findByIdAsAdminShouldReturnUser() throws Exception {
        var response = new UserResponse(5L, "João", "joao@test.com", UserRole.CUSTOMER.name(), LocalDateTime.now());
        when(userService.findById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/users/5").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João"));
    }

    @Test
    @DisplayName("GET /api/users/{id} com CUSTOMER deve retornar 403")
    void findByIdAsCustomerShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users/5").header("Authorization", customerAuthHeader(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} com ADMIN deve remover e retornar 204")
    void deleteAsAdminShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/users/5").header("Authorization", adminAuthHeader()))
                .andExpect(status().isNoContent());

        verify(userService).delete(5L);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} com CUSTOMER deve retornar 403 e não chamar o service")
    void deleteAsCustomerShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/users/5").header("Authorization", customerAuthHeader(1L)))
                .andExpect(status().isForbidden());

        verify(userService, never()).delete(org.mockito.ArgumentMatchers.anyLong());
    }
}
