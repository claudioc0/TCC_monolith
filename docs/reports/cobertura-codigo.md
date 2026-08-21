# Relatório de Cobertura de Código (Branch Coverage)

**Ferramenta:** JaCoCo 0.8.11
**Sistema:** monolith-ecommerce
**Commit de referência:** `a5fe3a0`
**Critério medido:** cobertura de ramos (branch coverage) — fração de desvios de
fluxo (`if`/`else`/`switch`/operadores curto-circuitados) exercitados pela suíte
de testes, mais rigoroso que cobertura de linha/comando.

## Resultado geral

| Métrica | Valor |
|---|---|
| Ramos cobertos | 1.785 / 1.982 |
| **Branch coverage** | **90,1%** |
| Execuções (determinismo) | 3, resultado idêntico em todas — ver `execution-log.md` |

## Cobertura por pacote

| Pacote | Ramos cobertos | Cobertura |
|---|---|---|
| `exception` | 140/140 | 100,0% |
| `orders.dto` | 104/104 | 100,0% |
| `orders.exception` | 25/25 | 100,0% |
| `products.dto` | 57/57 | 100,0% |
| `products.exception` | 27/27 | 100,0% |
| `reports.controller` | 13/13 | 100,0% |
| `reports.dto` | 24/24 | 100,0% |
| `reports.service` | 52/52 | 100,0% |
| `security` | 410/416 | 98,6% |
| `orders.entity` | 255/260 | 98,1% |
| `orders.service` | 173/178 | 97,2% |
| `users.entity` | 87/90 | 96,7% |
| `orders.controller` | 41/47 | 87,2% |
| `products.entity` | 54/66 | 81,8% |
| `users.service` | 115/150 | 76,7% |
| `products.service` | 102/147 | 69,4% |
| `users.dto` | 52/85 | 61,2% |
| `products.controller` | 25/45 | 55,6% |
| `users.controller` | 26/48 | 54,2% |
| `com.pucpr.tcc.monolith` (raiz/bootstrap) | 3/8 | 37,5% |
| **Total** | **1.785/1.982** | **90,1%** |

## Leitura dos dados

- **Pacotes em 100%**: DTOs, exceções e os dois controllers que ganharam teste
  de integração dedicado (`reports.controller`, e `orders.controller` perto
  disso, 87,2%) — evidência de que os testes de integração escritos para
  `OrderController`/`ReportController` cobriram efetivamente os ramos que antes
  ficavam sem exercício (eram 12,8% e 46,2% antes desses testes).
- **Pontos mais fracos**: `users.controller` (54,2%) e `products.controller`
  (55,6%) — nenhum dos dois tem teste de integração próprio ainda; `users`
  também não tem `UserServiceTest` cobrindo os ramos de erro. `users.dto`
  (61,2%) reflete DTOs com métodos de conversão pouco exercitados.
- **`com.pucpr.tcc.monolith`** (37,5%, só 8 ramos) é a classe de bootstrap
  (`MonolithApplication`) — poucos ramos reais, baixa prioridade.

## Metodologia

O plugin `jacoco-maven-plugin` está preso ao ciclo de vida padrão do Maven
(`prepare-agent` antes dos testes, `report` na fase `test`), então o relatório
é gerado automaticamente em toda execução de `mvn test`, sem passo manual
adicional.

```bash
mvn test
# relatório em target/site/jacoco/index.html
```

Um snapshot do relatório HTML completo (navegável por pacote/classe/linha)
está versionado em [`jacoco/index.html`](./jacoco/index.html).
