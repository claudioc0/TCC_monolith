package com.pucpr.tcc.ecommerce.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("Deve criar produto com dados válidos")
    void shouldCreateProductWithValidData() {
        Product p = new Product("Notebook", new BigDecimal("2500.00"), 10);
        assertThat(p.getName()).isEqualTo("Notebook");
        assertThat(p.getPrice()).isEqualByComparingTo("2500.00");
        assertThat(p.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Não deve criar produto com preço zero")
    void shouldRejectZeroPrice() {
        assertThatThrownBy(() -> new Product("Item", BigDecimal.ZERO, 5))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    @DisplayName("Não deve criar produto com preço negativo")
    void shouldRejectNegativePrice() {
        assertThatThrownBy(() -> new Product("Item", new BigDecimal("-1.00"), 5))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    @DisplayName("Deve reduzir estoque corretamente")
    void shouldDecreaseStock() {
        Product p = new Product("Mouse", new BigDecimal("50.00"), 10);
        p.decreaseStock(3);
        assertThat(p.getStockQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("Deve lançar InsufficientStockException quando solicitado maior que disponível")
    void shouldThrowInsufficientStockException() {
        Product p = new Product("Teclado", new BigDecimal("120.00"), 2);
        assertThatThrownBy(() -> p.decreaseStock(5))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção quando estoque é zero")
    void shouldThrowWhenStockIsZero() {
        Product p = new Product("Item", new BigDecimal("10.00"), 0);
        assertThatThrownBy(() -> p.decreaseStock(1))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("Deve permitir reduzir estoque até exatamente zero")
    void shouldAllowDecreaseToExactZero() {
        Product p = new Product("Item", new BigDecimal("10.00"), 5);
        p.decreaseStock(5);
        assertThat(p.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("hasStock retorna true quando suficiente")
    void hasStockReturnsTrueWhenSufficient() {
        Product p = new Product("Item", new BigDecimal("10.00"), 5);
        assertThat(p.hasStock(5)).isTrue();
        assertThat(p.hasStock(3)).isTrue();
    }

    @Test
    @DisplayName("hasStock retorna false quando insuficiente")
    void hasStockReturnsFalseWhenInsufficient() {
        Product p = new Product("Item", new BigDecimal("10.00"), 2);
        assertThat(p.hasStock(3)).isFalse();
    }

    @Test
    @DisplayName("Deve atualizar preço para valor válido")
    void shouldUpdatePriceToValidValue() {
        Product p = new Product("Item", new BigDecimal("10.00"), 5);
        p.updatePrice(new BigDecimal("20.00"));
        assertThat(p.getPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("Não deve atualizar preço para valor inválido")
    void shouldRejectInvalidPriceUpdate() {
        Product p = new Product("Item", new BigDecimal("10.00"), 5);
        assertThatThrownBy(() -> p.updatePrice(BigDecimal.ZERO))
                .isInstanceOf(InvalidProductException.class);
    }
}
