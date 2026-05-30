package com.pucpr.tcc.ecommerce.order.application;

import com.pucpr.tcc.ecommerce.customer.application.CustomerService;
import com.pucpr.tcc.ecommerce.order.domain.*;
import com.pucpr.tcc.ecommerce.product.application.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    // Monolith: direct in-memory injection — no HTTP calls
    private final CustomerService customerService;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository,
                        CustomerService customerService,
                        ProductService productService) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.productService = productService;
    }

    public Order checkout(CheckoutRequest request) {
        // Rule 1: customer must be active (in-memory call)
        if (!customerService.isActive(request.customerId())) {
            throw new InvalidOrderException(
                "Cliente inativo não pode realizar checkout. ID: " + request.customerId());
        }

        // Rule 2: all products must have available stock (in-memory call)
        for (CheckoutRequest.CheckoutItemRequest item : request.items()) {
            if (!productService.checkStock(item.productId(), item.quantity())) {
                throw new InvalidOrderException(
                    "Estoque insuficiente para o produto ID: " + item.productId());
            }
        }

        // Build order items using current product price (in-memory call)
        List<OrderItem> orderItems = request.items().stream()
                .map(item -> {
                    var product = productService.findById(item.productId());
                    return new OrderItem(item.productId(), item.quantity(), product.getPrice());
                })
                .toList();

        // Create order — progressive discount applied by domain entity
        Order order = new Order(request.customerId(), orderItems);
        Order saved = orderRepository.save(order);

        // Decrease stock after saving (in-memory call)
        request.items().forEach(item ->
                productService.decreaseStock(item.productId(), item.quantity()));

        return saved;
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public Order cancel(Long id) {
        Order order = findById(id);
        order.cancel();
        return orderRepository.save(order);
    }

    public Order pay(Long id) {
        Order order = findById(id);
        order.pay();
        return orderRepository.save(order);
    }
}
