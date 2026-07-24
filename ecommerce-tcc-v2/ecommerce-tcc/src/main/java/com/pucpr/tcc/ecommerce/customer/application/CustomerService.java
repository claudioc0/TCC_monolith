package com.pucpr.tcc.ecommerce.customer.application;

import com.pucpr.tcc.ecommerce.customer.domain.Customer;
import com.pucpr.tcc.ecommerce.customer.domain.CustomerNotFoundException;
import com.pucpr.tcc.ecommerce.customer.domain.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer create(String name, String email) {
        return customerRepository.save(new Customer(name, email));
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public void validateActiveCustomer(Long id) {
        findById(id).validateIsActive();
    }

    public boolean isActive(Long id) {
        return findById(id).isActive();
    }

    public Customer deactivate(Long id) {
        Customer c = findById(id);
        c.deactivate();
        return customerRepository.save(c);
    }

    public Customer activate(Long id) {
        Customer c = findById(id);
        c.activate();
        return customerRepository.save(c);
    }

    public void delete(Long id) {
        findById(id);
        customerRepository.deleteById(id);
    }
}
