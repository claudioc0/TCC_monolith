package com.pucpr.tcc.ecommerce.user.application;

import com.pucpr.tcc.ecommerce.user.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private User existing;

    @BeforeEach
    void setUp() { existing = new User("maria", "hashed-pass", Role.USER); }

    @Test
    @DisplayName("register deve salvar usuário com senha codificada")
    void registerShouldSaveWithEncodedPassword() {
        when(userRepository.existsByUsername("maria")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenReturn(existing);

        User result = userService.register("maria", "senha123", Role.USER);

        assertThat(result).isEqualTo(existing);
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register deve lançar exceção quando username já existe")
    void registerShouldThrowWhenUsernameExists() {
        when(userRepository.existsByUsername("maria")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("maria", "senha123", Role.USER))
                .isInstanceOf(DuplicateUsernameException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("authenticate deve retornar usuário quando credenciais são válidas")
    void authenticateShouldReturnUserWhenValid() {
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("senha123", "hashed-pass")).thenReturn(true);

        User result = userService.authenticate("maria", "senha123");

        assertThat(result).isEqualTo(existing);
    }

    @Test
    @DisplayName("authenticate deve lançar exceção quando username não existe")
    void authenticateShouldThrowWhenUsernameNotFound() {
        when(userRepository.findByUsername("desconhecido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.authenticate("desconhecido", "qualquer"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("authenticate deve lançar exceção quando senha é incorreta")
    void authenticateShouldThrowWhenPasswordWrong() {
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("errada", "hashed-pass")).thenReturn(false);

        assertThatThrownBy(() -> userService.authenticate("maria", "errada"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
