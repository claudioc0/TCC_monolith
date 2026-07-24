package com.pucpr.tcc.ecommerce.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(LocalDate startDate, LocalDate endDate, Long totalOrders,
                                    Long confirmedOrders, Long canceledOrders, BigDecimal totalRevenue,
                                    List<OrderSummary> orders) {}
