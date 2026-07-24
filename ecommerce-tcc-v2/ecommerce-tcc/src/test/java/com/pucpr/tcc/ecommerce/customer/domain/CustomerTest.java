package com.pucpr.tcc.ecommerce.customer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CustomerTest {

    @Test
    @DisplayName("Deve criar cliente ativo com dados válidos")
    void shouldCreateActiveCustomer() {
        Customer c = new Customer("João", "joao@email.com");
        assertThat(c.getName()).isEqualTo("João");
        assertThat(c.isActive()).isTrue();
    }

    @Test
    @DisplayName("Não deve criar cliente sem @")
    void shouldRejectEmailWithoutAtSign() {
        assertThatThrownBy(() -> new Customer("Test", "invalidemail.com"))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    @DisplayName("Não deve criar cliente sem domínio após @")
    void shouldRejectEmailWithoutDomain() {
        assertThatThrownBy(() -> new Customer("Test", "test@"))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    @DisplayName("Não deve criar cliente sem ponto no domínio")
    void shouldRejectEmailWithoutDotInDomain() {
        assertThatThrownBy(() -> new Customer("Test", "test@domainonly"))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    @DisplayName("Não deve criar cliente com email null")
    void shouldRejectNullEmail() {
        assertThatThrownBy(() -> new Customer("Test", null))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    @DisplayName("validateIsActive não lança exceção para cliente ativo")
    void validateIsActiveShouldNotThrowForActive() {
        Customer c = new Customer("Ativo", "ativo@test.com");
        assertThatNoException().isThrownBy(c::validateIsActive);
    }

    @Test
    @DisplayName("validateIsActive lança InactiveCustomerException para inativo")
    void validateIsActiveShouldThrowForInactive() {
        Customer c = new Customer("Inativo", "inativo@test.com");
        c.deactivate();
        assertThatThrownBy(c::validateIsActive)
                .isInstanceOf(InactiveCustomerException.class);
    }

    @Test
    @DisplayName("Deve desativar cliente")
    void shouldDeactivate() {
        Customer c = new Customer("Test", "test@test.com");
        c.deactivate();
        assertThat(c.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve reativar cliente")
    void shouldActivate() {
        Customer c = new Customer("Test", "test@test.com");
        c.deactivate();
        c.activate();
        assertThat(c.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar email com subdomínio")
    void shouldAcceptEmailWithSubdomain() {
        assertThatNoException()
                .isThrownBy(() -> new Customer("Test", "user@mail.company.com"));
    }
}
