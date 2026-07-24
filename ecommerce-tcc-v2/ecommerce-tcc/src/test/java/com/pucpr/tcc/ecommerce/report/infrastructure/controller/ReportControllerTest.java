package com.pucpr.tcc.ecommerce.report.infrastructure.controller;

import com.pucpr.tcc.ecommerce.report.dto.SalesReportResponse;
import com.pucpr.tcc.ecommerce.report.service.ReportService;
import com.pucpr.tcc.ecommerce.security.JwtService;
import com.pucpr.tcc.ecommerce.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes unitários para o ReportController.
 *
 * Foco: Isolar a camada de Controller e validar o tratamento de requisições HTTP,
 * incluindo a restrição de acesso a usuários com role ADMIN.
 *
 * Diretrizes de Qualidade:
 * - Branch Coverage: caminho feliz (200), exceção de negócio (400), autorização insuficiente (403).
 * - Esforço de Mock: @WebMvcTest isola o Controller; ReportService é mockado.
 *   SecurityConfig é importada explicitamente para que a regra hasRole("ADMIN") em /api/reports/**
 *   seja realmente exercitada neste slice de teste.
 * - Mutation Score: asserções verificam status HTTP e valores específicos no JSON de resposta.
 */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    // Necessário para o contexto de segurança do Spring: o JwtAuthenticationFilter
    // (um @Component Filter) é carregado automaticamente pelo @WebMvcTest e depende de JwtService.
    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Deve retornar 200 OK e o relatório de vendas quando as datas são válidas")
    @WithMockUser(roles = "ADMIN")
    void getSalesReport_WithValidDates_ShouldReturnOkAndReport() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        var mockResponse = new SalesReportResponse(
                startDate, endDate, 10L, 8L, 1L, new BigDecimal("5000.00"), Collections.emptyList()
        );

        when(reportService.generateReport(startDate, endDate)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders", is(10)))
                .andExpect(jsonPath("$.totalRevenue", is(5000.00)))
                .andExpect(jsonPath("$.startDate", is("2024-01-01")));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o serviço lança IllegalArgumentException")
    @WithMockUser(roles = "ADMIN")
    void getSalesReport_WhenServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        when(reportService.generateReport(startDate, endDate))
                .thenThrow(new IllegalArgumentException("A data de início não pode ser posterior à data de fim."));

        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2024-02-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado não tem role ADMIN")
    @WithMockUser(roles = "USER")
    void getSalesReport_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verifyNoInteractions(reportService);
    }
}
