package com.pucpr.tcc.monolith.exception;

import com.pucpr.tcc.monolith.orders.exception.InvalidStatusTransitionException;
import com.pucpr.tcc.monolith.orders.exception.OrderNotFoundException;
import com.pucpr.tcc.monolith.orders.entity.OrderStatus;
import com.pucpr.tcc.monolith.products.exception.InsufficientStockException;
import com.pucpr.tcc.monolith.products.exception.ProductNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleProductNotFound deve retornar 404 com a mensagem da excecao")
    void handleProductNotFoundShouldReturn404() {
        var response = handler.handleProductNotFound(new ProductNotFoundException(7L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).contains("7");
    }

    @Test
    @DisplayName("handleInsufficientStock deve retornar 422 com a mensagem da excecao")
    void handleInsufficientStockShouldReturn422() {
        var response = handler.handleInsufficientStock(new InsufficientStockException(3L, 10, 2));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().status()).isEqualTo(422);
    }

    @Test
    @DisplayName("handleOrderNotFound deve retornar 404 com a mensagem da excecao")
    void handleOrderNotFoundShouldReturn404() {
        var response = handler.handleOrderNotFound(new OrderNotFoundException(9L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("9");
    }

    @Test
    @DisplayName("handleInvalidStatusTransition deve retornar 422 com a mensagem da excecao")
    void handleInvalidStatusTransitionShouldReturn422() {
        var ex = new InvalidStatusTransitionException(5L, OrderStatus.ENTREGUE, OrderStatus.PENDENTE);
        var response = handler.handleInvalidStatusTransition(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().message()).contains("ENTREGUE").contains("PENDENTE");
    }

    @Test
    @DisplayName("handleBadCredentials deve retornar 401 com mensagem fixa, ignorando a mensagem original")
    void handleBadCredentialsShouldReturn401WithFixedMessage() {
        var response = handler.handleBadCredentials(new BadCredentialsException("detalhe interno"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo("E-mail ou senha inválidos.");
    }

    @Test
    @DisplayName("handleAccessDenied deve retornar 403 com mensagem fixa")
    void handleAccessDeniedShouldReturn403WithFixedMessage() {
        var response = handler.handleAccessDenied(new AccessDeniedException("detalhe interno"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("Acesso negado.");
    }

    @Test
    @DisplayName("handleValidationErrors deve retornar 400 com o mapa de campos invalidos")
    void handleValidationErrorsShouldReturn400WithFieldMap() throws Exception {
        // Excecao real (sem mock): o Mockito nao consegue instrumentar
        // MethodArgumentNotValidException em algumas JDKs.
        var bindingResult = new BeanPropertyBindingResult(new Object(), "productRequest");
        bindingResult.addError(new FieldError("productRequest", "price", "O preço deve ser maior que zero."));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoAlvoDaValidacao", String.class);
        var ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Erro de validação nos campos enviados.");
        assertThat(response.getBody().errors()).containsEntry("price", "O preço deve ser maior que zero.");
    }

    @Test
    @DisplayName("handleIllegalArgument deve retornar 400 com a mensagem da excecao")
    void handleIllegalArgumentShouldReturn400() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("E-mail já cadastrado."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("E-mail já cadastrado.");
    }

    @Test
    @DisplayName("handleGeneral deve retornar 500 com mensagem fixa, sem vazar detalhes internos")
    void handleGeneralShouldReturn500WithGenericMessage() {
        var response = handler.handleGeneral(new RuntimeException("stacktrace sensivel"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message())
                .isEqualTo("Erro interno do servidor. Tente novamente mais tarde.")
                .doesNotContain("stacktrace sensivel");
    }

    /** Alvo ficticio usado apenas para construir o MethodParameter da excecao acima. */
    @SuppressWarnings("unused")
    private void metodoAlvoDaValidacao(String price) {
        // sem corpo: serve so como assinatura para a reflexao
    }
}
