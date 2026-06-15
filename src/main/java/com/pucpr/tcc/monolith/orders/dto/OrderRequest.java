package com.pucpr.tcc.monolith.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * DTO de entrada para criação de Pedido.
 */
public record OrderRequest(

    @NotEmpty(message = "O pedido deve conter pelo menos um item.")
    @Valid
    List<OrderItemRequest> items
) {
    /**
     * DTO aninhado para cada item do pedido.
     */
    public record OrderItemRequest(

        @NotNull(message = "O ID do produto é obrigatório.")
        Long productId,

        @NotNull(message = "A quantidade é obrigatória.")
        @Min(value = 1, message = "A quantidade deve ser no mínimo 1.")
        Integer quantity
    ) {}
}
