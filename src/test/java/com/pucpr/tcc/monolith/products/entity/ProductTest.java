package com.pucpr.tcc.monolith.products.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a entidade Product isolada do Spring/JPA: construtor, getters e os
 * setters usados pelo fluxo de atualização do ProductService.
 */
class ProductTest {

    @Test
    @DisplayName("construtor deve preencher name, description, price e stockQuantity")
    void constructorShouldPopulateAllFields() {
        Product product = new Product("Mouse", "Mouse sem fio", new BigDecimal("99.90"), 10);

        assertThat(product.getName()).isEqualTo("Mouse");
        assertThat(product.getDescription()).isEqualTo("Mouse sem fio");
        assertThat(product.getPrice()).isEqualByComparingTo("99.90");
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("setName deve sobrescrever o nome")
    void setNameShouldOverwriteName() {
        Product product = new Product("Mouse", "desc", BigDecimal.TEN, 5);
        product.setName("Mouse Gamer");
        assertThat(product.getName()).isEqualTo("Mouse Gamer");
    }

    @Test
    @DisplayName("setDescription deve sobrescrever a descrição")
    void setDescriptionShouldOverwriteDescription() {
        Product product = new Product("Mouse", "desc antiga", BigDecimal.TEN, 5);
        product.setDescription("desc nova");
        assertThat(product.getDescription()).isEqualTo("desc nova");
    }

    @Test
    @DisplayName("setPrice deve sobrescrever o preço")
    void setPriceShouldOverwritePrice() {
        Product product = new Product("Mouse", "desc", new BigDecimal("50.00"), 5);
        product.setPrice(new BigDecimal("75.50"));
        assertThat(product.getPrice()).isEqualByComparingTo("75.50");
    }

    @Test
    @DisplayName("setStockQuantity deve sobrescrever a quantidade em estoque")
    void setStockQuantityShouldOverwriteStock() {
        Product product = new Product("Mouse", "desc", BigDecimal.TEN, 5);
        product.setStockQuantity(20);
        assertThat(product.getStockQuantity()).isEqualTo(20);
    }
}
