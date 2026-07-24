package com.pucpr.tcc.ecommerce.customer.application;

import com.pucpr.tcc.ecommerce.customer.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @InjectMocks CustomerService customerService;

    private Customer active;

    @BeforeEach
    void setUp() { active = new Customer("Maria", "maria@test.com"); }

    @Test
    @DisplayName("create deve salvar cliente")
    void createShouldSave() {
        when(customerRepository.save(any())).thenReturn(active);
        Customer result = customerService.create("Maria", "maria@test.com");
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("findById lança exceção para id inexistente")
    void findByIdThrowsWhenNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    @DisplayName("isActive retorna true para cliente ativo")
    void isActiveTrueForActive() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(active));
        assertThat(customerService.isActive(1L)).isTrue();
    }

    @Test
    @DisplayName("isActive retorna false para cliente inativo")
    void isActiveFalseForInactive() {
        active.deactivate();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(active));
        assertThat(customerService.isActive(1L)).isFalse();
    }

    @Test
    @DisplayName("validateActiveCustomer lança exceção para inativo")
    void validateActiveCustomerThrowsForInactive() {
        active.deactivate();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(active));
        assertThatThrownBy(() -> customerService.validateActiveCustomer(1L))
                .isInstanceOf(InactiveCustomerException.class);
    }
}
