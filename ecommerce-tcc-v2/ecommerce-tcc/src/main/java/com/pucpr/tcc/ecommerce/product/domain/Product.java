package com.pucpr.tcc.ecommerce.product.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    protected Product() {}

    public Product(String name, BigDecimal price, Integer stockQuantity) {
        validatePrice(price);
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    // ---- Business Rules (PITest targets) ----

    public void decreaseStock(int quantity) {
        if (quantity > this.stockQuantity) {
            throw new InsufficientStockException(this.id, quantity, this.stockQuantity);
        }
        this.stockQuantity -= quantity;
    }

    public void updatePrice(BigDecimal newPrice) {
        validatePrice(newPrice);
        this.price = newPrice;
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductException("O preço do produto deve ser maior que zero.");
        }
    }

    public boolean hasStock(int quantity) {
        return this.stockQuantity >= quantity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setName(String name) { this.name = name; }
}
