package com.pucpr.tcc.monolith.reports.service;

import com.pucpr.tcc.monolith.orders.dto.OrderResponse;
import com.pucpr.tcc.monolith.orders.entity.OrderStatus;
import com.pucpr.tcc.monolith.orders.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private OrderService orderService;
    @InjectMocks private ReportService reportService;

    private static OrderResponse order(Long id, OrderStatus status) {
        return new OrderResponse(id, 1L, List.of(), new BigDecimal("100.00"), status,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("generateReport deve agregar totais a partir do OrderService")
    void generateReportShouldAggregateTotals() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        LocalDateTime expectedStart = startDate.atStartOfDay();
        LocalDateTime expectedEnd = endDate.atTime(LocalTime.MAX);

        List<OrderResponse> orders = List.of(
                order(1L, OrderStatus.ENTREGUE),
                order(2L, OrderStatus.CANCELADO),
                order(3L, OrderStatus.PENDENTE)
        );

        when(orderService.findByPeriod(expectedStart, expectedEnd)).thenReturn(orders);
        when(orderService.countDeliveredByPeriod(expectedStart, expectedEnd)).thenReturn(1L);
        when(orderService.countCanceledByPeriod(expectedStart, expectedEnd)).thenReturn(1L);
        when(orderService.sumRevenueByPeriod(expectedStart, expectedEnd)).thenReturn(new BigDecimal("250.00"));

        var response = reportService.generateReport(startDate, endDate);

        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.endDate()).isEqualTo(endDate);
        assertThat(response.totalOrders()).isEqualTo(3L);
        assertThat(response.deliveredOrders()).isEqualTo(1L);
        assertThat(response.canceledOrders()).isEqualTo(1L);
        assertThat(response.totalRevenue()).isEqualByComparingTo("250.00");
        assertThat(response.orders()).containsExactlyElementsOf(orders);

        verify(orderService).findByPeriod(expectedStart, expectedEnd);
        verify(orderService).countDeliveredByPeriod(expectedStart, expectedEnd);
        verify(orderService).countCanceledByPeriod(expectedStart, expectedEnd);
        verify(orderService).sumRevenueByPeriod(expectedStart, expectedEnd);
    }

    @Test
    @DisplayName("generateReport deve converter startDate para 00:00:00 e endDate para 23:59:59.999999999")
    void generateReportShouldConvertDatesToFullDayBoundaries() {
        LocalDate startDate = LocalDate.of(2026, 3, 10);
        LocalDate endDate = LocalDate.of(2026, 3, 10);

        when(orderService.findByPeriod(any(), any())).thenReturn(List.of());
        when(orderService.countDeliveredByPeriod(any(), any())).thenReturn(0L);
        when(orderService.countCanceledByPeriod(any(), any())).thenReturn(0L);
        when(orderService.sumRevenueByPeriod(any(), any())).thenReturn(BigDecimal.ZERO);

        reportService.generateReport(startDate, endDate);

        verify(orderService).findByPeriod(
                LocalDateTime.of(2026, 3, 10, 0, 0, 0),
                LocalDateTime.of(2026, 3, 10, 23, 59, 59, 999_999_999));
    }

    @Test
    @DisplayName("generateReport deve retornar totalOrders zero quando nao ha pedidos no periodo")
    void generateReportShouldReturnZeroTotalsForEmptyPeriod() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 31);

        when(orderService.findByPeriod(any(), any())).thenReturn(List.of());
        when(orderService.countDeliveredByPeriod(any(), any())).thenReturn(0L);
        when(orderService.countCanceledByPeriod(any(), any())).thenReturn(0L);
        when(orderService.sumRevenueByPeriod(any(), any())).thenReturn(BigDecimal.ZERO);

        var response = reportService.generateReport(startDate, endDate);

        assertThat(response.totalOrders()).isZero();
        assertThat(response.deliveredOrders()).isZero();
        assertThat(response.canceledOrders()).isZero();
        assertThat(response.totalRevenue()).isEqualByComparingTo("0");
        assertThat(response.orders()).isEmpty();
    }
}
