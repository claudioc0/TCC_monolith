package com.pucpr.tcc.ecommerce.customer.domain;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id) { super("Cliente não encontrado com id: " + id); }
}
