package com.pucpr.tcc.monolith.reports.controller;

import com.pucpr.tcc.monolith.reports.dto.SalesReportResponse;
import com.pucpr.tcc.monolith.reports.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller de Relatórios.
 *
 * Acesso exclusivo para usuários com role ADMIN
 * (configurado também no SecurityConfig como camada extra de proteção).
 *
 * Exemplo de chamada:
 * GET /api/reports/sales?startDate=2024-01-01&endDate=2024-01-31
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Gera um relatório de vendas para o período informado.
     *
     * @param startDate data de início no formato ISO (yyyy-MM-dd)
     * @param endDate   data de fim no formato ISO (yyyy-MM-dd)
     * @return sumário de vendas com lista de pedidos no período
     */
    @GetMapping("/sales")
    public ResponseEntity<SalesReportResponse> salesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.generateReport(startDate, endDate));
    }
}
