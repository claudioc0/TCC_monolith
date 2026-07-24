package com.pucpr.tcc.ecommerce.user.domain;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("Username já cadastrado: " + username);
    }
}
