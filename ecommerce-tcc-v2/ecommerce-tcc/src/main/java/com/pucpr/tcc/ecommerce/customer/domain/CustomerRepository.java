package com.pucpr.tcc.ecommerce.customer.domain;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(Long id);
    void deleteById(Long id);
    List<Customer> findAll();
}
