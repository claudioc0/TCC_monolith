package com.pucpr.tcc.ecommerce.user.domain;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Usuário ou senha inválidos.");
    }
}
