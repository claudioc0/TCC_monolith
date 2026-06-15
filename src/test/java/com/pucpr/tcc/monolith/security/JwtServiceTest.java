package com.pucpr.tcc.monolith.security;

import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para a geração e validação de tokens JWT.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Injeta os valores de application.properties via ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "secret",
                "3cFqr9Xv8pNzLmKwEoJbDsYtAhUgRiPl2nVxOzBmQkHyWdCu");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);

        user = new User("João", "joao@test.com", "$2a$hashed", UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("generateToken deve retornar token não-nulo e não-vazio")
    void generateTokenShouldReturnNonBlankToken() {
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUsername deve retornar o email do usuário")
    void extractUsernameShouldReturnEmail() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("joao@test.com");
    }

    @Test
    @DisplayName("isTokenValid deve retornar true para token válido")
    void isTokenValidShouldReturnTrueForValidToken() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid deve retornar false para token de outro usuário")
    void isTokenValidShouldReturnFalseForDifferentUser() {
        String token = jwtService.generateToken(user);
        User otherUser = new User("Outro", "outro@test.com", "hash", UserRole.CUSTOMER);
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid deve retornar false para token expirado")
    void isTokenValidShouldReturnFalseForExpiredToken() {
        // Define expiração de -1ms (já expirado)
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1L);
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid deve retornar false para token malformado")
    void isTokenValidShouldReturnFalseForMalformedToken() {
        assertThat(jwtService.isTokenValid("token.invalido.aqui", user)).isFalse();
    }
}
