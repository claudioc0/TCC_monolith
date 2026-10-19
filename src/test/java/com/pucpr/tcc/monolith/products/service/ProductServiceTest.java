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
        assertThat(response.description()).isEqualTo("Notebook gamer");
        assertThat(response.price()).isEqualByComparingTo("3500.00");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("findAll deve retornar todos os produtos cadastrados")
    void findAllShouldReturnAllProducts() {
        Product other = new Product("Mouse", "Mouse sem fio", new BigDecimal("80.00"), 20);
        when(productRepository.findAll()).thenReturn(java.util.List.of(product, other));

        var response = productService.findAll();

        assertThat(response).hasSize(2);
        assertThat(response).extracting("name").containsExactly("Notebook", "Mouse");
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
    @DisplayName("decreaseStock deve permitir quando a quantidade solicitada é exatamente igual ao estoque")
    void decreaseStockShouldSucceedWhenQuantityEqualsStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.decreaseStock(1L, 10); // estoque inicial é exatamente 10

        assertThat(product.getStockQuantity()).isEqualTo(0);
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

    @Test
    @DisplayName("searchByName deve retornar os produtos cujo nome contém o termo buscado")
    void searchByNameShouldReturnMatchingProducts() {
        when(productRepository.findByNameContainingIgnoreCase("note"))
                .thenReturn(java.util.List.of(product));

        var response = productService.searchByName("note");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name()).isEqualTo("Notebook");
    }

    @Test
    @DisplayName("searchByName deve retornar lista vazia quando nenhum produto corresponde")
    void searchByNameShouldReturnEmptyListWhenNoMatch() {
        when(productRepository.findByNameContainingIgnoreCase("inexistente"))
                .thenReturn(java.util.List.of());

        var response = productService.searchByName("inexistente");

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("update deve sobrescrever todos os campos do produto e retornar o DTO atualizado")
    void updateShouldOverwriteAllFieldsAndReturnResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        var request = new ProductRequest("Notebook Pro", "Novo descrição", new BigDecimal("4200.00"), 5);

        var response = productService.update(1L, request);

        assertThat(response.name()).isEqualTo("Notebook Pro");
        assertThat(response.description()).isEqualTo("Novo descrição");
        assertThat(response.price()).isEqualByComparingTo("4200.00");
        assertThat(response.stockQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("update deve lançar ProductNotFoundException para id inexistente")
    void updateShouldThrowForUnknownId() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        var request = new ProductRequest("X", "Y", BigDecimal.TEN, 1);

        assertThatThrownBy(() -> productService.update(99L, request))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete deve remover o produto quando ele existe")
    void deleteShouldRemoveWhenProductExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete deve lançar ProductNotFoundException e não remover quando o produto não existe")
    void deleteShouldThrowAndNotRemoveForUnknownId() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }
}
