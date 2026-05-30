package com.pucpr.tcc.ecommerce.order.infrastructure.dto;

import java.util.List;

public record OrderRequest(Long customerId, List<OrderItemRequest> items) {}
