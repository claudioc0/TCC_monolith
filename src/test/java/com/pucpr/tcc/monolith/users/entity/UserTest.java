package com.pucpr.tcc.monolith.users.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a entidade User isolada do Spring, inclusive o contrato que ela
 * implementa de {@link org.springframework.security.core.userdetails.UserDetails}
 * — é essa implementação que o Spring Security usa para autenticar via JWT,
 * então um erro aqui (ex.: authority sem prefixo ROLE_) quebra o login inteiro.
 */
class UserTest {

    @Test
    @DisplayName("getUsername deve retornar o email, não o nome")
    void getUsernameShouldReturnEmail() {
        User user = new User("João Silva", "joao@email.com", "hash", UserRole.CUSTOMER);
        assertThat(user.getUsername()).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("getPassword deve retornar o hash armazenado")
    void getPasswordShouldReturnStoredHash() {
        User user = new User("João Silva", "joao@email.com", "hash-bcrypt", UserRole.CUSTOMER);
        assertThat(user.getPassword()).isEqualTo("hash-bcrypt");
    }

    @Test
    @DisplayName("getAuthorities deve conter ROLE_CUSTOMER para um usuário CUSTOMER")
    void getAuthoritiesShouldPrefixRoleForCustomer() {
        User user = new User("João Silva", "joao@email.com", "hash", UserRole.CUSTOMER);
        assertThat(user.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("getAuthorities deve conter ROLE_ADMIN para um usuário ADMIN")
    void getAuthoritiesShouldPrefixRoleForAdmin() {
        User user = new User("Admin", "admin@tcc.com", "hash", UserRole.ADMIN);
        assertThat(user.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("conta deve sempre estar não expirada, não bloqueada, com credenciais válidas e habilitada")
    void accountFlagsShouldAlwaysBeTrue() {
        User user = new User("João Silva", "joao@email.com", "hash", UserRole.CUSTOMER);
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("construtor deve preencher name, email, password e role")
    void constructorShouldPopulateAllFields() {
        User user = new User("João Silva", "joao@email.com", "hash", UserRole.CUSTOMER);
        assertThat(user.getName()).isEqualTo("João Silva");
        assertThat(user.getEmail()).isEqualTo("joao@email.com");
        assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("onCreate (callback @PrePersist) deve preencher createdAt com o instante atual")
    void onCreateShouldPopulateCreatedAt() {
        User user = new User("João Silva", "joao@email.com", "hash", UserRole.CUSTOMER);

        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull().isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }
}
