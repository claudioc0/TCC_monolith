package com.pucpr.tcc.monolith.products.service;

import com.pucpr.tcc.monolith.products.dto.ProductRequest;
import com.pucpr.tcc.monolith.products.entity.Product;
import com.pucpr.tcc.monolith.products.exception.InsufficientStockException;
import com.pucpr.tcc.monolith.products.exception.ProductNotFoundException;
import com.pucpr.tcc.monolith.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product("Notebook", "Notebook gamer", new BigDecimal("3500.00"), 10);
    }

    @Test
    @DisplayName("create deve salvar e retornar ProductResponse")
    void createShouldSaveAndReturnResponse() {
        when(productRepository.save(any(Product.class))).thenReturn(product);
        var request = new ProductRequest("Notebook", "Notebook gamer", new BigDecimal("3500.00"), 10);

        var response = productService.create(request);

        assertThat(response.name()).isEqualTo("Notebook");
        assertThat(response.price()).isEqualByComparingTo("3500.00");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("findById deve lançar ProductNotFoundException para id inexistente")
    void findByIdShouldThrowForUnknownId() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("decreaseStock deve reduzir estoque corretamente")
    void decreaseStockShouldReduceQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.decreaseStock(1L, 3);

        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("decreaseStock deve lançar InsufficientStockException quando estoque insuficiente")
    void decreaseStockShouldThrowWhenInsufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.decreaseStock(1L, 20))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    @DisplayName("increaseStock deve devolver estoque corretamente")
    void increaseStockShouldAddQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.increaseStock(1L, 5);

        assertThat(product.getStockQuantity()).isEqualTo(15);
    }
}
