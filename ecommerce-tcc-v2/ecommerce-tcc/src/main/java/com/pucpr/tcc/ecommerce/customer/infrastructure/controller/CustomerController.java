package com.pucpr.tcc.ecommerce.customer.infrastructure.controller;

import com.pucpr.tcc.ecommerce.customer.application.CustomerService;
import com.pucpr.tcc.ecommerce.customer.infrastructure.dto.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@RequestBody CustomerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerResponse.from(customerService.create(req.name(), req.email())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(CustomerResponse.from(customerService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> findAll() {
        return ResponseEntity.ok(customerService.findAll().stream().map(CustomerResponse::from).toList());
    }

    @GetMapping("/{id}/active")
    public ResponseEntity<Boolean> isActive(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.isActive(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CustomerResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(CustomerResponse.from(customerService.deactivate(id)));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<CustomerResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(CustomerResponse.from(customerService.activate(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
