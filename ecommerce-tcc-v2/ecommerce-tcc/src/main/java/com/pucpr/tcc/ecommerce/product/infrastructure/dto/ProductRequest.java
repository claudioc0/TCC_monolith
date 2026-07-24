package com.pucpr.tcc.ecommerce.product.infrastructure.dto;

import java.math.BigDecimal;

public record ProductRequest(String name, BigDecimal price, Integer stockQuantity) {}
