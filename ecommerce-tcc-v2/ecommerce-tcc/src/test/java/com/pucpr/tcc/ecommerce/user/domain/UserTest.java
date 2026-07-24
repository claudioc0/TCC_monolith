package com.pucpr.tcc.ecommerce.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Deve criar usuário com role explícita")
    void shouldCreateUserWithExplicitRole() {
        User u = new User("maria", "hash123", Role.ADMIN);
        assertThat(u.getUsername()).isEqualTo("maria");
        assertThat(u.getPassword()).isEqualTo("hash123");
        assertThat(u.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Deve assumir role USER quando role é null")
    void shouldDefaultToUserRoleWhenNull() {
        User u = new User("joao", "hash456", null);
        assertThat(u.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Não deve criar usuário com username vazio")
    void shouldRejectBlankUsername() {
        assertThatThrownBy(() -> new User("  ", "hash", Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("Não deve criar usuário com username null")
    void shouldRejectNullUsername() {
        assertThatThrownBy(() -> new User(null, "hash", Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("Não deve criar usuário com password vazio")
    void shouldRejectBlankPassword() {
        assertThatThrownBy(() -> new User("maria", " ", Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("Não deve criar usuário com password null")
    void shouldRejectNullPassword() {
        assertThatThrownBy(() -> new User("maria", null, Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("hasRole retorna true para a role correspondente")
    void hasRoleShouldReturnTrueForMatchingRole() {
        User u = new User("admin", "hash", Role.ADMIN);
        assertThat(u.hasRole(Role.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("hasRole retorna false para role diferente")
    void hasRoleShouldReturnFalseForDifferentRole() {
        User u = new User("comum", "hash", Role.USER);
        assertThat(u.hasRole(Role.ADMIN)).isFalse();
    }
}
