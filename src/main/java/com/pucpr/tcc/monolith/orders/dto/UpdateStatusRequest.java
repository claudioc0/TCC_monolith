package com.pucpr.tcc.monolith.orders.dto;

import com.pucpr.tcc.monolith.orders.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para atualização de status de um Pedido.
 */
public record UpdateStatusRequest(
    @NotNull(message = "O novo status é obrigatório.")
    OrderStatus newStatus
) {}
