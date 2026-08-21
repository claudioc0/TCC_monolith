# Relatório de Mutation Score

**Ferramenta:** PITest 1.15.8 (+ `pitest-junit5-plugin`)
**Sistema:** monolith-ecommerce
**Commit de referência:** `a5fe3a0`
**Critério medido:** fração de mutantes (alterações sintáticas mínimas e
deliberadas no bytecode — inverter uma condição, trocar uma constante, remover
uma chamada) que a suíte de testes detecta (mata) ao falhar diante deles.

## Resultado geral

| Status | Mutantes | % |
|---|---|---|
| `KILLED` (detectado por algum teste) | 140 | 81,9% |
| `NO_COVERAGE` (código nunca alcançado por nenhum teste) | 28 | 16,4% |
| `SURVIVED` (alcançado, mas nenhum teste falhou) | 3 | 1,8% |
| **Total gerado** | **171** | — |

**Test Strength** (mortos / alcançados = 140/143): **97,9%** — isola a
qualidade das asserções nos trechos que os testes já exercitam, separando-a
do problema de cobertura ausente. Praticamente todo mutante que os testes
alcançam hoje é detectado; o que resta é majoritariamente falta de cobertura,
não asserção fraca.

Execuções (determinismo): 3 rodadas, resultado idêntico em todas (140/171) —
ver `execution-log.md`.

## Mutation score por pacote

| Pacote | Mortos | Score |
|---|---|---|
| `exception` | 10/10 | 100,0% |
| `orders.dto` | 2/2 | 100,0% |
| `products.dto` | 1/1 | 100,0% |
| `products.entity` | 7/7 | 100,0% |
| `reports.controller` | 1/1 | 100,0% |
| `reports.service` | 1/1 | 100,0% |
| `orders.service` | 16/17 | 94,1% |
| `orders.entity` | 27/29 | 93,1% |
| `users.entity` | 11/12 | 91,7% |
| `security` | 38/42 | 90,5% |
| `orders.controller` | 5/6 | 83,3% |
| `products.service` | 11/18 | 61,1% |
| `users.dto` | 1/2 | 50,0% |
| `users.service` | 4/9 | 44,4% |
| `products.controller` | 3/7 | 42,9% |
| `users.controller` | 2/7 | 28,6% |
| **Total** | **140/171** | **81,9%** |

## Mutantes SURVIVED — os 3 que restam, documentados individualmente

Nenhum foi ignorado silenciosamente; cada um tem uma razão específica pela
qual matá-lo exigiria alterar código de produção (não só adicionar teste) ou
teria custo desproporcional ao ganho:

| Classe / método | Mutação | Por que sobrevive |
|---|---|---|
| `JwtService#isTokenExpired` | boolean sempre `false` | Efetivamente inalcançável no fluxo normal: o parser do JJWT já lança `ExpiredJwtException` para um token expirado antes de essa comparação ser avaliada. |
| `ApplicationConfig#authenticationManager` | retorno `null` | Depende de `AuthenticationConfiguration` interno do Spring, só construído de verdade dentro de um `@SpringBootTest` completo. |
| `SecurityConfig#securityFilterChain` | retorno `null` | Depende de `HttpSecurity` interno do Spring — mesma categoria acima. |

As duas últimas são código de *wiring* de framework, categoria classicamente
aceita como residual em mutation testing (ver Jia & Harman, 2011).

## Onde está a cobertura ausente (os 28 `NO_COVERAGE`)

| Pacote | Mutantes sem cobertura |
|---|---|
| `products.service` | 7 |
| `users.service` | 5 |
| `users.controller` | 5 |
| `products.controller` | 4 |
| `orders.entity` | 2 |
| `orders.service` | 1 |
| `security` | 1 |
| `users.entity` | 1 |
| `orders.controller` | 1 |
| `users.dto` | 1 |

Concentra exatamente nos mesmos pontos fracos identificados no relatório de
branch coverage (`users.controller`, `products.controller`, `users.service`)
— confirma que o próximo ganho de mutation score vem de mais testes de
integração/unitários nesses pacotes, não de reforçar asserções existentes.

## Histórico: rodada baseline vs. rodada de refinamento

| Rodada | Mortos | Score | SURVIVED |
|---|---|---|---|
| Baseline (antes dos testes de integração de Order/Report) | 90/171 | 52,6% | 34 |
| Após testes de integração (Order/Report) | 112/171 | 65,5% | 28 |
| Após reforço dirigido pelos SURVIVED do PITest | **140/171** | **81,9%** | **3** |

## Metodologia

Diferente do JaCoCo, o PITest **não** está preso ao ciclo de vida padrão do
`mvn test` — precisa do goal explícito:

```bash
mvn org.pitest:pitest-maven:mutationCoverage
# relatório em target/pit-reports/index.html
```

Um snapshot do relatório HTML completo (com o detalhe de cada mutante gerado)
está versionado em [`pitest/index.html`](./pitest/index.html).
