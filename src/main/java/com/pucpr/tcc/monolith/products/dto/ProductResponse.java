package com.pucpr.tcc.monolith.products.dto;

import com.pucpr.tcc.monolith.products.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de saída para Produto.
 * Nunca expõe a entidade JPA diretamente na API.
 */
public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /** Factory method: mapeia entidade → DTO de resposta. */
    public static ProductResponse from(Product p) {
        return new ProductResponse(
            p.getId(), p.getName(), p.getDescription(),
            p.getPrice(), p.getStockQuantity(),
            p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
