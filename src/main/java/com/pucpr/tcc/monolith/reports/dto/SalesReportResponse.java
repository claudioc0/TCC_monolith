package com.pucpr.tcc.monolith.reports.dto;

import com.pucpr.tcc.monolith.orders.dto.OrderResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de saída para o Relatório de Pedidos por Período.
 * Retorna um sumário de vendas e a lista de pedidos no intervalo.
 */
public record SalesReportResponse(

    /** Data de início do período consultado. */
    LocalDate startDate,

    /** Data de fim do período consultado. */
    LocalDate endDate,

    /** Total de pedidos criados no período (incluindo cancelados). */
    long totalOrders,

    /** Pedidos ENTREGUES no período. */
    long deliveredOrders,

    /** Pedidos CANCELADOS no período. */
    long canceledOrders,

    /** Receita total: soma dos totalAmount de pedidos não-cancelados. */
    BigDecimal totalRevenue,

    /** Lista completa dos pedidos no período. */
    List<OrderResponse> orders
) {}
