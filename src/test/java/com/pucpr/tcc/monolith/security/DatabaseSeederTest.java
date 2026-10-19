package com.pucpr.tcc.monolith.security;

import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import com.pucpr.tcc.monolith.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testa o {@code CommandLineRunner} de {@link DatabaseSeeder} isolado do
 * Spring: cada um dos dois usuários semente (admin e cliente) só deve ser
 * criado se ainda não existir — o teste cobre os dois ramos desse
 * {@code if} para cada usuário, algo que os testes de integração sozinhos
 * não garantem (eles só exercitam o caminho "ainda não existe", de banco
 * limpo).
 */
@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private final DatabaseSeeder seeder = new DatabaseSeeder();

    @Test
    @DisplayName("seedAdmin deve criar admin e cliente quando nenhum dos dois existe")
    void seedAdminShouldCreateBothUsersWhenNoneExist() throws Exception {
        when(userRepository.findByEmail("admin@tcc.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");

        CommandLineRunner runner = seeder.seedAdmin(userRepository, passwordEncoder);
        runner.run();

        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("seedAdmin não deve recriar o admin quando ele já existe")
    void seedAdminShouldSkipAdminWhenAlreadyExists() throws Exception {
        User existingAdmin = new User("Administrador", "admin@tcc.com", "hash", UserRole.ADMIN);
        when(userRepository.findByEmail("admin@tcc.com")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");

        CommandLineRunner runner = seeder.seedAdmin(userRepository, passwordEncoder);
        runner.run();

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("seedAdmin não deve recriar nenhum dos dois usuários quando ambos já existem")
    void seedAdminShouldSkipBothWhenBothAlreadyExist() throws Exception {
        User existingAdmin = new User("Administrador", "admin@tcc.com", "hash", UserRole.ADMIN);
        User existingCustomer = new User("João Silva", "joao@email.com", "hash", UserRole.CUSTOMER);
        when(userRepository.findByEmail("admin@tcc.com")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(existingCustomer));

        CommandLineRunner runner = seeder.seedAdmin(userRepository, passwordEncoder);
        runner.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
