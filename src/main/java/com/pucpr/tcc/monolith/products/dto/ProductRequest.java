package com.pucpr.tcc.monolith.products.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO de entrada para criação e atualização de Produto.
 * As anotações de validação garantem que dados inválidos
 * sejam rejeitados antes de atingir a camada de Service.
 */
public record ProductRequest(

    @NotBlank(message = "O nome do produto é obrigatório.")
    String name,

    String description,

    @NotNull(message = "O preço é obrigatório.")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
    BigDecimal price,

    @NotNull(message = "A quantidade em estoque é obrigatória.")
    @Min(value = 0, message = "O estoque não pode ser negativo.")
    Integer stockQuantity
) {}
