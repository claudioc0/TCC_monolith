package com.pucpr.tcc.ecommerce.user.domain;

public class InvalidUserException extends RuntimeException {
    public InvalidUserException(String message) { super(message); }
}
