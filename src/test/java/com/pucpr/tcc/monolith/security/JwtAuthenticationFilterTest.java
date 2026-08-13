package com.pucpr.tcc.monolith.security;

import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do filtro JWT, chamando {@code shouldNotFilter} e
 * {@code doFilterInternal} diretamente (ambos protected, mesma package do teste).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        // Construído aqui (não como inicializador de campo) porque os @Mock só existem
        // depois que o MockitoExtension injeta os campos, o que acontece antes de @BeforeEach.
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("shouldNotFilter deve retornar true para rotas /api/auth/**")
    void shouldNotFilterShouldReturnTrueForAuthRoutes() {
        when(request.getServletPath()).thenReturn("/api/auth/login");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter deve retornar false para rotas protegidas")
    void shouldNotFilterShouldReturnFalseForProtectedRoutes() {
        when(request.getServletPath()).thenReturn("/api/orders");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("doFilterInternal com token válido deve autenticar e popular os detalhes da requisição")
    void doFilterInternalWithValidTokenShouldAuthenticateWithDetails() throws Exception {
        User user = new User("Maria", "maria@test.com", "hash", UserRole.CUSTOMER);
        when(request.getHeader("Authorization")).thenReturn("Bearer token.valido.aqui");
        when(jwtService.extractUsername("token.valido.aqui")).thenReturn("maria@test.com");
        when(userDetailsService.loadUserByUsername("maria@test.com")).thenReturn(user);
        when(jwtService.isTokenValid("token.valido.aqui", user)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.getDetails()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal sem header Authorization deve seguir sem autenticar")
    void doFilterInternalWithoutHeaderShouldSkipAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("doFilterInternal com token malformado deve seguir sem autenticar")
    void doFilterInternalWithMalformedTokenShouldSkipAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token.invalido");
        when(jwtService.extractUsername("token.invalido")).thenThrow(new RuntimeException("malformed"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("doFilterInternal com token de usuário inexistente deve seguir sem autenticar")
    void doFilterInternalWithUnknownUserShouldSkipAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token.usuario.inexistente");
        when(jwtService.extractUsername("token.usuario.inexistente")).thenReturn("fantasma@test.com");
        when(userDetailsService.loadUserByUsername("fantasma@test.com"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("não encontrado"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal com token inválido (assinatura/expiração) não deve autenticar")
    void doFilterInternalWithInvalidTokenShouldSkipAuthentication() throws Exception {
        User user = new User("Maria", "maria@test.com", "hash", UserRole.CUSTOMER);
        when(request.getHeader("Authorization")).thenReturn("Bearer token.expirado");
        when(jwtService.extractUsername("token.expirado")).thenReturn("maria@test.com");
        when(userDetailsService.loadUserByUsername("maria@test.com")).thenReturn(user);
        when(jwtService.isTokenValid("token.expirado", user)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
