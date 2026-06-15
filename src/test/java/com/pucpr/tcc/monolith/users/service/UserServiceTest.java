package com.pucpr.tcc.monolith.users.service;

import com.pucpr.tcc.monolith.security.JwtService;
import com.pucpr.tcc.monolith.users.dto.LoginRequest;
import com.pucpr.tcc.monolith.users.dto.RegisterRequest;
import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import com.pucpr.tcc.monolith.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("register deve salvar usuário com senha hasheada e retornar token")
    void registerShouldHashPasswordAndReturnToken() {
        var request = new RegisterRequest("João", "joao@email.com", "senha123");
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.mock");

        var response = userService.register(request);

        assertThat(response.token()).isEqualTo("jwt.token.mock");
        assertThat(response.email()).isEqualTo("joao@email.com");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.role()).isEqualTo("CUSTOMER");

        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register deve lançar exceção se e-mail já estiver cadastrado")
    void registerShouldThrowIfEmailAlreadyExists() {
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(new RegisterRequest("João", "joao@email.com", "senha123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-mail já cadastrado");
    }

    @Test
    @DisplayName("login deve retornar token para credenciais válidas")
    void loginShouldReturnTokenForValidCredentials() {
        User user = new User("João", "joao@email.com", "$2a$hashed", UserRole.CUSTOMER);
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.mock");

        var response = userService.login(new LoginRequest("joao@email.com", "senha123"));

        assertThat(response.token()).isEqualTo("jwt.token.mock");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login deve lançar BadCredentialsException para senha inválida")
    void loginShouldThrowForInvalidCredentials() {
        doThrow(new BadCredentialsException("Credenciais inválidas"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() ->
                userService.login(new LoginRequest("joao@email.com", "senha_errada")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("findById deve lançar exceção para usuário inexistente")
    void findByIdShouldThrowForUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuário não encontrado");
    }
}
