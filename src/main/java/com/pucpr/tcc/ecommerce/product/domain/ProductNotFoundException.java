package com.pucpr.tcc.ecommerce.product.domain;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Produto não encontrado com id: " + id);
    }
}
