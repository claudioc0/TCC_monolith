package com.pucpr.tcc.ecommerce.product.application;

import com.pucpr.tcc.ecommerce.product.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductService productService;

    private Product existing;

    @BeforeEach
    void setUp() { existing = new Product("Monitor", new BigDecimal("800.00"), 10); }

    @Test
    @DisplayName("create deve salvar produto no repositório")
    void createShouldSave() {
        when(productRepository.save(any())).thenReturn(existing);
        Product result = productService.create("Monitor", new BigDecimal("800.00"), 10);
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("findById deve lançar exceção para id inexistente")
    void findByIdShouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("decreaseStock deve delegar à entidade e salvar")
    void decreaseStockShouldDelegateAndSave() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(existing);
        productService.decreaseStock(1L, 3);
        assertThat(existing.getStockQuantity()).isEqualTo(7);
        verify(productRepository).save(existing);
    }

    @Test
    @DisplayName("checkStock deve retornar false quando insuficiente")
    void checkStockFalseWhenInsufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        assertThat(productService.checkStock(1L, 100)).isFalse();
    }

    @Test
    @DisplayName("checkStock deve retornar true quando suficiente")
    void checkStockTrueWhenSufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        assertThat(productService.checkStock(1L, 5)).isTrue();
    }
}
