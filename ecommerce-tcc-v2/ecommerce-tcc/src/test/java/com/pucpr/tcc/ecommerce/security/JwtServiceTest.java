package com.pucpr.tcc.ecommerce.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-with-at-least-256-bits-for-hs256-signing", 3_600_000L);

    @Test
    @DisplayName("Deve gerar um token não vazio")
    void shouldGenerateNonEmptyToken() {
        String token = jwtService.generateToken("maria", "ADMIN");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Deve extrair o username correto do token gerado")
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateToken("joao", "USER");
        assertThat(jwtService.extractUsername(token)).isEqualTo("joao");
    }

    @Test
    @DisplayName("Deve extrair a role correta do token gerado")
    void shouldExtractRolesFromToken() {
        String token = jwtService.generateToken("admin", "ADMIN");
        assertThat(jwtService.extractRoles(token)).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("Deve validar como verdadeiro um token gerado corretamente")
    void shouldValidateGeneratedTokenAsTrue() {
        String token = jwtService.generateToken("valido", "USER");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Deve validar como falso um token malformado")
    void shouldInvalidateMalformedToken() {
        assertThat(jwtService.isTokenValid("token-invalido-nao-assinado")).isFalse();
    }

    @Test
    @DisplayName("Deve validar como falso um token assinado com outra chave")
    void shouldInvalidateTokenSignedWithDifferentKey() {
        JwtService otherService = new JwtService(
                "another-completely-different-secret-key-256-bits-long-value", 3_600_000L);
        String token = otherService.generateToken("hacker", "ADMIN");
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Deve validar como falso um token expirado")
    void shouldInvalidateExpiredToken() {
        JwtService expiredService = new JwtService(
                "test-secret-key-with-at-least-256-bits-for-hs256-signing", -1000L);
        String token = expiredService.generateToken("expirado", "USER");
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
