package com.pucpr.tcc.ecommerce.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    private static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Order() {}

    public Order(Long customerId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Um pedido deve ter pelo menos um item.");
        }
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.totalAmount = calculateTotal(items);
    }

    // ---- Business Rule: Progressive Discount (PITest target) ----
    private BigDecimal calculateTotal(List<OrderItem> items) {
        BigDecimal subtotal = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (subtotal.compareTo(DISCOUNT_THRESHOLD) > 0) {
            BigDecimal discount = subtotal.multiply(DISCOUNT_RATE);
            return subtotal.subtract(discount);
        }
        return subtotal;
    }

    public void cancel() {
        if (this.status == OrderStatus.PAID) {
            throw new InvalidOrderException("Não é possível cancelar um pedido já pago.");
        }
        this.status = OrderStatus.CANCELED;
    }

    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderException("Apenas pedidos PENDING podem ser pagos.");
        }
        this.status = OrderStatus.PAID;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
