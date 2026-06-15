package com.pucpr.tcc.monolith.reports.service;

import com.pucpr.tcc.monolith.orders.dto.OrderResponse;
import com.pucpr.tcc.monolith.orders.entity.OrderStatus;
import com.pucpr.tcc.monolith.orders.repository.OrderRepository;
import com.pucpr.tcc.monolith.reports.dto.SalesReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Serviço responsável pela geração de relatórios de vendas.
 *
 * Recebe uma data de início e fim (LocalDate) e retorna um
 * sumário de pedidos no período com métricas de negócio.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Gera o relatório de vendas para o período [startDate, endDate].
     *
     * A conversão de LocalDate para LocalDateTime usa:
     * - início do dia (00:00:00) para startDate
     * - fim do dia (23:59:59.999999999) para endDate
     * garantindo que todos os pedidos do dia final sejam incluídos.
     *
     * @param startDate data de início (inclusive).
     * @param endDate   data de fim (inclusive).
     * @return {@link SalesReportResponse} com sumário e lista de pedidos.
     */
    public SalesReportResponse generateReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.atTime(LocalTime.MAX);

        // Busca todos os pedidos do período
        List<OrderResponse> orders = orderRepository
                .findByCreatedAtBetween(start, end)
                .stream()
                .map(OrderResponse::from)
                .toList();

        long totalOrders    = orders.size();
        long deliveredOrders = orderRepository.countByStatusAndPeriod(OrderStatus.ENTREGUE, start, end);
        long canceledOrders  = orderRepository.countByStatusAndPeriod(OrderStatus.CANCELADO, start, end);

        BigDecimal totalRevenue = orderRepository.sumRevenueByPeriod(start, end);

        return new SalesReportResponse(
            startDate,
            endDate,
            totalOrders,
            deliveredOrders,
            canceledOrders,
            totalRevenue,
            orders
        );
    }
}
