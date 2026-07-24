package com.pucpr.tcc.ecommerce.customer.infrastructure.dto;

import com.pucpr.tcc.ecommerce.customer.domain.Customer;

public record CustomerResponse(Long id, String name, String email, Boolean active) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getActive());
    }
}
