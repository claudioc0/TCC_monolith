package com.pucpr.tcc.monolith.security;

import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import com.pucpr.tcc.monolith.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para os beans de segurança expostos em {@link ApplicationConfig}.
 *
 * Não usa contexto Spring — instancia a classe de configuração diretamente e
 * chama os métodos @Bean como métodos de fábrica comuns, verificando o
 * comportamento real de cada um (não apenas que "não é nulo").
 */
@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock private UserRepository userRepository;

    private ApplicationConfig config;

    @BeforeEach
    void setUp() {
        config = new ApplicationConfig(userRepository);
    }

    @Test
    @DisplayName("passwordEncoder deve retornar um BCryptPasswordEncoder funcional")
    void passwordEncoderShouldReturnWorkingBCryptEncoder() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String hash = encoder.encode("senha123");
        assertThat(encoder.matches("senha123", hash)).isTrue();
        assertThat(encoder.matches("senha_errada", hash)).isFalse();
    }

    @Test
    @DisplayName("userDetailsService deve retornar o usuário quando o e-mail existe")
    void userDetailsServiceShouldReturnUserWhenFound() {
        User user = new User("Maria", "maria@test.com", "hash", UserRole.CUSTOMER);
        when(userRepository.findByEmail("maria@test.com")).thenReturn(Optional.of(user));

        UserDetailsService uds = config.userDetailsService();

        assertThat(uds.loadUserByUsername("maria@test.com")).isEqualTo(user);
    }

    @Test
    @DisplayName("userDetailsService deve lançar UsernameNotFoundException quando o e-mail não existe")
    void userDetailsServiceShouldThrowWhenNotFound() {
        when(userRepository.findByEmail("fantasma@test.com")).thenReturn(Optional.empty());

        UserDetailsService uds = config.userDetailsService();

        assertThatThrownBy(() -> uds.loadUserByUsername("fantasma@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("authenticationProvider deve autenticar com sucesso quando a senha confere")
    void authenticationProviderShouldAuthenticateWithCorrectPassword() {
        PasswordEncoder realEncoder = new BCryptPasswordEncoder();
        User user = new User("Maria", "maria@test.com", realEncoder.encode("senha123"), UserRole.CUSTOMER);
        when(userRepository.findByEmail("maria@test.com")).thenReturn(Optional.of(user));

        AuthenticationProvider provider = config.authenticationProvider();
        var result = provider.authenticate(
                new UsernamePasswordAuthenticationToken("maria@test.com", "senha123"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(user);
    }

    @Test
    @DisplayName("authenticationProvider deve rejeitar senha incorreta")
    void authenticationProviderShouldRejectWrongPassword() {
        PasswordEncoder realEncoder = new BCryptPasswordEncoder();
        User user = new User("Maria", "maria@test.com", realEncoder.encode("senha123"), UserRole.CUSTOMER);
        when(userRepository.findByEmail("maria@test.com")).thenReturn(Optional.of(user));

        AuthenticationProvider provider = config.authenticationProvider();

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken("maria@test.com", "senha_errada")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
