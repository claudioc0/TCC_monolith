package com.pucpr.tcc.ecommerce.customer.infrastructure.repository;

import com.pucpr.tcc.ecommerce.customer.domain.Customer;
import com.pucpr.tcc.ecommerce.customer.domain.CustomerRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCustomerRepository extends JpaRepository<Customer, Long>, CustomerRepository {}
