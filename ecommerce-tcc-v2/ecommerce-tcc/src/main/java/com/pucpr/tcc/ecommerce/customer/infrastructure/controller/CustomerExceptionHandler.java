package com.pucpr.tcc.ecommerce.customer.infrastructure.controller;

import com.pucpr.tcc.ecommerce.customer.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.pucpr.tcc.ecommerce.customer")
public class CustomerExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String,String>> notFound(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<Map<String,String>> invalidEmail(InvalidEmailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InactiveCustomerException.class)
    public ResponseEntity<Map<String,String>> inactive(InactiveCustomerException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }
}
