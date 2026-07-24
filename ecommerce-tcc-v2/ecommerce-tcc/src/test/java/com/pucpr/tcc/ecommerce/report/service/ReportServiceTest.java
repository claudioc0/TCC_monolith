package com.pucpr.tcc.ecommerce.report.service;

import com.pucpr.tcc.ecommerce.order.domain.Order;
import com.pucpr.tcc.ecommerce.order.domain.OrderItem;
import com.pucpr.tcc.ecommerce.order.domain.OrderRepository;
import com.pucpr.tcc.ecommerce.report.dto.SalesReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock OrderRepository orderRepository;
    @InjectMocks ReportService reportService;

    @Test
    @DisplayName("Deve lançar exceção quando data de início é posterior à data de fim")
    void shouldThrowWhenStartDateAfterEndDate() {
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        assertThatThrownBy(() -> reportService.generateReport(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data de início não pode ser posterior à data de fim.");

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Deve aceitar data de início igual à data de fim")
    void shouldAcceptEqualStartAndEndDate() {
        when(orderRepository.findAll()).thenReturn(List.of());
        LocalDate today = LocalDate.now();

        assertThatNoException().isThrownBy(() -> reportService.generateReport(today, today));
    }

    @Test
    @DisplayName("Deve incluir pedidos do período e calcular totais corretamente")
    void shouldIncludeOrdersWithinRangeAndComputeTotals() {
        Order confirmed = new Order(1L, List.of(new OrderItem(1L, 1, new BigDecimal("100.00"))));
        confirmed.confirm();
        Order canceled = new Order(2L, List.of(new OrderItem(1L, 1, new BigDecimal("50.00"))));
        canceled.cancel();
        Order pending = new Order(3L, List.of(new OrderItem(1L, 1, new BigDecimal("30.00"))));

        when(orderRepository.findAll()).thenReturn(List.of(confirmed, canceled, pending));

        LocalDate today = LocalDate.now();
        SalesReportResponse report = reportService.generateReport(today, today);

        assertThat(report.totalOrders()).isEqualTo(3L);
        assertThat(report.confirmedOrders()).isEqualTo(1L);
        assertThat(report.canceledOrders()).isEqualTo(1L);
        assertThat(report.totalRevenue()).isEqualByComparingTo("100.00");
        assertThat(report.orders()).hasSize(3);
        assertThat(report.startDate()).isEqualTo(today);
        assertThat(report.endDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("Pedidos enviados e entregues também contam como receita confirmada")
    void shippedAndDeliveredOrdersShouldCountAsConfirmedRevenue() {
        Order shipped = new Order(1L, List.of(new OrderItem(1L, 1, new BigDecimal("200.00"))));
        shipped.confirm();
        shipped.ship();
        Order delivered = new Order(2L, List.of(new OrderItem(1L, 1, new BigDecimal("300.00"))));
        delivered.confirm();
        delivered.ship();
        delivered.deliver();

        when(orderRepository.findAll()).thenReturn(List.of(shipped, delivered));

        LocalDate today = LocalDate.now();
        SalesReportResponse report = reportService.generateReport(today, today);

        assertThat(report.confirmedOrders()).isEqualTo(2L);
        assertThat(report.totalRevenue()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Deve excluir pedidos criados fora do período informado")
    void shouldExcludeOrdersOutsideRange() {
        Order order = new Order(1L, List.of(new OrderItem(1L, 1, new BigDecimal("100.00"))));
        when(orderRepository.findAll()).thenReturn(List.of(order));

        LocalDate past = LocalDate.now().minusDays(10);
        SalesReportResponse report = reportService.generateReport(past, past);

        assertThat(report.totalOrders()).isEqualTo(0L);
        assertThat(report.confirmedOrders()).isEqualTo(0L);
        assertThat(report.canceledOrders()).isEqualTo(0L);
        assertThat(report.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.orders()).isEmpty();
    }

    @Test
    @DisplayName("Receita total deve considerar apenas pedidos confirmados (ou além)")
    void totalRevenueShouldConsiderOnlyConfirmedOrders() {
        Order pendingOnly = new Order(1L, List.of(new OrderItem(1L, 1, new BigDecimal("999.00"))));
        when(orderRepository.findAll()).thenReturn(List.of(pendingOnly));

        LocalDate today = LocalDate.now();
        SalesReportResponse report = reportService.generateReport(today, today);

        assertThat(report.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
