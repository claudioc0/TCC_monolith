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

## Conclusão (testes unitários/integração/arquitetura)

3 execuções, 3 resultados idênticos (96/96, 0 falhas, 0 erros) — nenhuma
evidência de teste instável (*flaky*) na suíte atual. A variação de duração
(29–33s) é normal (JIT warm-up, I/O do H2, contenção de CPU) e não afeta o
resultado.

## Branch Coverage (JaCoCo) — acumulado

| # | Data/hora | Commit | Cobertura |
|---|---|---|---|
| 1 | 2026-08-13 | `67d5a7b` | 90,1% (1.785/1.982) |
| 2 | 2026-08-21 19:21 | `3c46469` | 90,1% (1.785/1.982) |

Sem variação — esperado, já que nenhum código de produção mudou entre as execuções.

## Mutation Score (PITest) — acumulado

| # | Data/hora | Commit | Mortos | Score |
|---|---|---|---|---|
| 1 | 2026-08-13 | `67d5a7b` | 140/171 | 81,9% |
| 2 | 2026-08-21 | `3c46469` | 140/171 | 81,9% (82% arredondado no relatório do PIT) |
| 3 | 2026-08-21 | `3c46469` | 140/171 | 81,9% (82% arredondado no relatório do PIT) |

3 rodadas, mesmo resultado exato (140/171 mortos) — mutation testing com
PITest é determinístico neste projeto (não há dependência de tempo,
aleatoriedade ou concorrência nos testes que afetaria o resultado).

## Conclusão geral

As três métricas do protocolo GQM (testes, branch coverage, mutation score)
foram reexecutadas de forma independente e produziram resultado idêntico em
todas as rodadas. Isso descarta flakiness como explicação para os números
reportados no documento de métricas — o estado atual (96 testes, 90,1%
branch coverage, 81,9% mutation score, 0 violações arquiteturais) é estável
e reproduzível.

## Como reproduzir

```bash
mvn test                                          # testes + JaCoCo
mvn org.pitest:pitest-maven:mutationCoverage      # PITest
```
