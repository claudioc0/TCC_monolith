package com.pucpr.tcc.ecommerce.customer.domain;

public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String email) { super("E-mail inválido: " + email); }
}
