package com.pucpr.tcc.monolith.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa {@link SecurityConfig#corsConfigurationSource()} como um método de fábrica
 * comum, sem subir contexto Spring — usa {@link MockHttpServletRequest} apenas para
 * exercitar a resolução real de {@link org.springframework.web.cors.UrlBasedCorsConfigurationSource},
 * garantindo que o registro em "/**" e os métodos/headers permitidos realmente
 * estejam configurados (não apenas que o bean "não é nulo").
 */
class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig(Mockito.mock(JwtAuthenticationFilter.class));

    @Test
    @DisplayName("corsConfigurationSource deve permitir os métodos e headers esperados para qualquer rota")
    void corsConfigurationSourceShouldExposeExpectedMethodsAndHeaders() {
        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");

        CorsConfiguration corsConfig = source.getCorsConfiguration(request);

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(corsConfig.getAllowedHeaders()).containsExactly("*");
    }
}
