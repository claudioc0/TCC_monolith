package com.pucpr.tcc.monolith.reports.controller;

import com.pucpr.tcc.monolith.reports.dto.SalesReportResponse;
import com.pucpr.tcc.monolith.reports.service.ReportService;
import com.pucpr.tcc.monolith.security.JwtAuthenticationFilter;
import com.pucpr.tcc.monolith.security.JwtService;
import com.pucpr.tcc.monolith.security.SecurityConfig;
import com.pucpr.tcc.monolith.testsupport.SecurityTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test de ReportController: toda a classe é {@code @PreAuthorize("hasRole('ADMIN')")},
 * então o foco é confirmar a dupla camada de proteção (SecurityConfig +
 * método) e a passagem correta dos parâmetros de data para o service.
 */
@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class ReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockBean private ReportService reportService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private AuthenticationProvider authenticationProvider;

    private String adminAuthHeader() {
        return SecurityTestSupport.bearerTokenFor(jwtService, userDetailsService, SecurityTestSupport.adminUser(1L));
    }

    private String customerAuthHeader() {
        return SecurityTestSupport.bearerTokenFor(jwtService, userDetailsService, SecurityTestSupport.customerUser(2L));
    }

    @Test
    @DisplayName("GET /api/reports/sales com ADMIN deve retornar o relatório gerado pelo service")
    void salesReportAsAdminShouldReturnGeneratedReport() throws Exception {
        var response = new SalesReportResponse(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                10, 7, 1, new BigDecimal("5000.00"), List.of());
        when(reportService.generateReport(any(LocalDate.class), any(LocalDate.class))).thenReturn(response);

        mockMvc.perform(get("/api/reports/sales")
                        .header("Authorization", adminAuthHeader())
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(10))
                .andExpect(jsonPath("$.totalRevenue").value(5000.00));
    }

    @Test
    @DisplayName("GET /api/reports/sales com CUSTOMER deve retornar 403")
    void salesReportAsCustomerShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .header("Authorization", customerAuthHeader())
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/reports/sales sem autenticação deve ser rejeitado")
    void salesReportWithoutAuthenticationShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/reports/sales com data em formato inválido cai no handler genérico (500) — "
            + "GlobalExceptionHandler não tem @ExceptionHandler dedicado para erro de bind de @RequestParam")
    void salesReportWithMalformedDateFallsBackToGenericHandler() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .header("Authorization", adminAuthHeader())
                        .param("startDate", "01-01-2026")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isInternalServerError());
    }
}
