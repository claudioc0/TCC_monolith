package com.pucpr.tcc.ecommerce.product.infrastructure.dto;

import com.pucpr.tcc.ecommerce.product.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, Integer stockQuantity) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStockQuantity());
    }
}
