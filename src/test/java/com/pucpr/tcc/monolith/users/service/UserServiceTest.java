package com.pucpr.tcc.monolith.users.service;

import com.pucpr.tcc.monolith.security.JwtService;
import com.pucpr.tcc.monolith.users.dto.LoginRequest;
import com.pucpr.tcc.monolith.users.dto.RegisterRequest;
import com.pucpr.tcc.monolith.users.dto.UserResponse;
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

import java.util.List;
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

    @Test
    @DisplayName("findById deve mapear o usuário encontrado para UserResponse")
    void findByIdShouldReturnMappedResponseWhenFound() {
        User user = new User("João", "joao@email.com", "$2a$hashed", UserRole.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(1L);

        assertThat(response.name()).isEqualTo("João");
        assertThat(response.email()).isEqualTo("joao@email.com");
        assertThat(response.role()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("findAll deve mapear todos os usuários retornados pelo repositório")
    void findAllShouldReturnMappedListOfUsers() {
        User joao = new User("João", "joao@email.com", "$2a$hashed", UserRole.CUSTOMER);
        User admin = new User("Admin", "admin@tcc.com", "$2a$hashed", UserRole.ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(joao, admin));

        List<UserResponse> responses = userService.findAll();

        assertThat(responses)
                .hasSize(2)
                .extracting(UserResponse::email)
                .containsExactly("joao@email.com", "admin@tcc.com");
    }

    @Test
    @DisplayName("delete deve remover o usuário quando ele existe")
    void deleteShouldRemoveWhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete deve lançar exceção e não remover quando o usuário não existe")
    void deleteShouldThrowAndNotRemoveWhenUserDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("login deve lançar IllegalStateException se o usuário sumir após autenticar")
    void loginShouldThrowIllegalStateWhenUserMissingAfterAuthentication() {
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.login(new LoginRequest("joao@email.com", "senha123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Usuário não encontrado após autenticação");
    }
}
