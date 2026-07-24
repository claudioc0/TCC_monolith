package com.pucpr.tcc.ecommerce.report.service;

import com.pucpr.tcc.ecommerce.order.domain.Order;
import com.pucpr.tcc.ecommerce.order.domain.OrderRepository;
import com.pucpr.tcc.ecommerce.order.domain.OrderStatus;
import com.pucpr.tcc.ecommerce.report.dto.OrderSummary;
import com.pucpr.tcc.ecommerce.report.dto.SalesReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public SalesReportResponse generateReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de fim.");
        }

        List<Order> ordersInRange = orderRepository.findAll().stream()
                .filter(order -> isWithinRange(order, startDate, endDate))
                .toList();

        long totalOrders = ordersInRange.size();
        long confirmedOrders = ordersInRange.stream()
                .filter(order -> order.getStatus().isConfirmedOrLater())
                .count();
        long canceledOrders = ordersInRange.stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELED)
                .count();

        BigDecimal totalRevenue = ordersInRange.stream()
                .filter(order -> order.getStatus().isConfirmedOrLater())
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderSummary> summaries = ordersInRange.stream().map(OrderSummary::from).toList();

        return new SalesReportResponse(startDate, endDate, totalOrders, confirmedOrders, canceledOrders,
                totalRevenue, summaries);
    }

    private boolean isWithinRange(Order order, LocalDate startDate, LocalDate endDate) {
        LocalDate createdDate = order.getCreatedAt().toLocalDate();
        return !createdDate.isBefore(startDate) && !createdDate.isAfter(endDate);
    }
}
