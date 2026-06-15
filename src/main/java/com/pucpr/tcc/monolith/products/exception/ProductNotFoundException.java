package com.pucpr.tcc.monolith.products.exception;

/**
 * Lançada quando um produto não é encontrado pelo ID informado.
 * Mapeada para HTTP 404 pelo GlobalExceptionHandler.
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Produto não encontrado com id: " + id);
    }
}
