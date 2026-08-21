# Log de Execuções da Suíte de Testes

Registro acumulado de execuções de `mvn test` (JUnit 5: testes unitários +
integração + arquitetura), para evidenciar estabilidade/determinismo da
suíte — nenhuma execução deve apresentar resultado diferente das outras.

| # | Data/hora | Commit | Testes | Falhas | Erros | Duração | Resultado |
|---|---|---|---|---|---|---|---|
| 1 | 2026-08-13 | `67d5a7b` | 96 | 0 | 0 | não registrada | BUILD SUCCESS |
| 2 | 2026-08-21 19:21 | `3c46469` | 96 | 0 | 0 | 32.8s | BUILD SUCCESS |
| 3 | 2026-08-21 19:22 | `3c46469` | 96 | 0 | 0 | 30.0s | BUILD SUCCESS |

## Detalhamento por classe (idêntico nas 3 execuções)

| Classe de teste | Testes |
|---|---|
| `GlobalExceptionHandlerTest` | 9 |
| `AuthIntegrationTest` | 8 |
| `OrderIntegrationTest` | 13 |
| `ProductIntegrationTest` | 5 |
| `ReportIntegrationTest` | 3 |
| `OrderStatusTransitionTest` | 11 |
| `OrderServiceTest` | 6 |
| `ProductServiceTest` | 7 |
| `ReportServiceTest` | 3 |
| `ApplicationConfigTest` | 5 |
| `JwtAuthenticationFilterTest` | 7 |
| `JwtServiceTest` | 6 |
| `SecurityConfigTest` | 1 |
| `UserServiceTest` | 5 |
| `CommonArchitectureRulesTest` | 3 |
| `DomainBoundaryArchitectureTest` | 4 |
| **Total** | **96** |

## Conclusão

3 execuções, 3 resultados idênticos (96/96, 0 falhas, 0 erros) — nenhuma
evidência de teste instável (*flaky*) na suíte atual. A variação de duração
(29–33s) é normal (JIT warm-up, I/O do H2, contenção de CPU) e não afeta o
resultado.

## Como reproduzir

```bash
mvn test
```
