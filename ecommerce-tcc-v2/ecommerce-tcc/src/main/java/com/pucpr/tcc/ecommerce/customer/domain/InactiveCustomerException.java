package com.pucpr.tcc.ecommerce.customer.domain;

public class InactiveCustomerException extends RuntimeException {
    public InactiveCustomerException(Long id) {
        super("Cliente inativo não pode realizar checkout. ID: " + id);
    }
}
