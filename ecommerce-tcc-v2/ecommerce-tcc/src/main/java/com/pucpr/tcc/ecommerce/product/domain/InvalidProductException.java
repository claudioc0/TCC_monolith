package com.pucpr.tcc.ecommerce.product.domain;

public class InvalidProductException extends RuntimeException {
    public InvalidProductException(String message) { super(message); }
}
