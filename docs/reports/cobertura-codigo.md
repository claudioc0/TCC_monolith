# Relatório de Cobertura de Código (Branch Coverage)

**Ferramenta:** JaCoCo 0.8.11
**Sistema:** monolith-ecommerce
**Commit de referência:** ver `execution-log.md`
**Critério medido:** cobertura de ramos (branch coverage) — fração de desvios de
fluxo (`if`/`else`/`switch`/operadores curto-circuitados) exercitados pela suíte
de testes.

## ⚠️ Correção de metodologia (registrada)

Uma medição anterior deste relatório reportou "branch coverage 90,1%
(1.785/1.982)". Esse número está **errado**: foram lidas as colunas
`INSTRUCTION_MISSED`/`INSTRUCTION_COVERED` do `jacoco.csv` (posições 4 e 5),
rotuladas como se fossem `BRANCH_MISSED`/`BRANCH_COVERED` (as colunas certas,
posições 6 e 7). 1.785/1.982 é cobertura de **instruções de bytecode**, não de
ramos — são grandezas de ordens de magnitude completamente diferentes (1.982
"ramos" não existem neste projeto; o sistema inteiro tem 42).

O erro foi encontrado por um integrante do grupo (Eric) ao refazer a medição de
forma independente, comparando contra o `jacoco.csv` bruto. Fica registrado
aqui por completo, incluindo a tabela de diagnóstico, porque a diferença **não
é cosmética**: muda a conclusão da comparação com a versão em microsserviços
(que reporta 64/64 = 100% de ramos — comparar 90,1% de instruções com 100% de
ramos é comparar contadores diferentes).

| Como estava registrado | O que o dado realmente é |
|---|---|
| "branch coverage 90,1% (1.785/1.982)" | cobertura de **instruções** (1.785/1.982 = 90,1%) |
| **Valor correto de ramos** | **34/42 = 81,0%** (após fechar o gap da §5) |

## Resultado geral (corrigido)

JaCoCo reporta cinco contadores independentes — qual se reporta como "a"
cobertura muda a conclusão. Os cinco, na medição atual:

| Contador | Cobertos | Total | Cobertura |
|---|---|---|---|
| **Ramos (branches) — métrica oficial do protocolo** | **34** | **42** | **81,0%** |
| Instruções (bytecode) | 1.787 | 1.982 | 90,2% |
| Linhas | 424 | 467 | 90,8% |

## Cobertura de ramos por pacote

Só pacotes com lógica condicional têm ramos — DTOs, repositórios (interfaces
Spring Data) e a maioria dos controllers (delegam pro Service e devolvem
`ResponseEntity`, sem `if`) não aparecem na tabela porque não têm ramo a
cobrir, não porque estejam mal testados.

| Pacote | Ramos cobertos | Cobertura |
|---|---|---|
| `exception` | 2/2 | 100,0% |
| `orders.entity` | 12/12 | 100,0% |
| `orders.service` | 4/4 | 100,0% |
| `products.service` | 2/2 | 100,0% |
| `security` | 12/18 | 66,7% |
| `users.service` | 2/4 | 50,0% |
| **Total** | **34/42** | **81,0%** |

## Por que a cobertura de ramos discrimina pouco neste projeto

O sistema inteiro tem só 42 ramos em 467 linhas — a lógica condicional fica
concentrada em três lugares: máquina de estados do pedido (`OrderStatus`),
validação de estoque (`ProductService`) e o filtro JWT (`JwtAuthenticationFilter`).
O resto do código é estruturalmente linear: controllers finos, DTOs `record`
com método de fábrica, repositórios como interface. Nesse perfil, cobertura de
ramos satura rápido — poucos testes bem escolhidos levam o número perto de
100% sem dizer muito sobre a qualidade das asserções. É por isso que o
protocolo usa quatro métricas e não confia só nesta: **mutation score** e
**densidade de violações arquiteturais** discriminam melhor (ver os outros
dois relatórios desta pasta).

O mesmo raciocínio se aplica à versão em microsserviços (64 ramos no total) —
com denominadores tão pequenos dos dois lados, a diferença percentual entre
as arquiteturas pesa menos na conclusão do que mutation score e violações.

## Ramos ainda sem cobertura

| Classe | Ramos em aberto | Observação |
|---|---|---|
| `security.JwtAuthenticationFilter` | 3 | Combinações da guarda que decide se a requisição segue autenticada — o ponto mais sensível por ser código de segurança. |
| `security.DatabaseSeeder` | 2 | Só cria usuários quando ainda não existem; nos testes o banco sempre começa vazio. Fora do escopo do PITest também. |
| `users.service.UserService` | 2 | Checagem de existência dentro de `delete`, sem teste que a exercite. |
| `security.JwtService` | 1 | Comparação de expiração do token — na prática inalcançável, o parser do JJWT lança `ExpiredJwtException` antes (mesmo ponto reaparece como mutante sobrevivente no relatório de mutation score). |

O gap de `OrderStatus` (transição a partir de `CONFIRMADO` sem teste direto
para os casos bloqueados) que existia numa medição anterior foi fechado —
ver `OrderStatusTransitionTest` (`confirmado_to_pendente_blocked`,
`confirmado_to_entregue_blocked`).

## Determinismo da medição

Verificado por dois métodos independentes: comparação do percentual final
entre execuções (3 rodadas, ver `execution-log.md`) e, de forma mais rigorosa,
comparação **byte a byte via hash MD5 do `jacoco.csv`** entre execuções
consecutivas — as três rodadas produziram arquivos idênticos, confirmando que
cada classe, linha e ramo foi coberto exatamente da mesma forma em todas.

## Comparação com a versão em microsserviços

| Métrica | Monólito | Microsserviços | Observação |
|---|---|---|---|
| Cobertura de ramos | 34/42 = 81,0% | 64/64 = 100,0% | Mesma ferramenta e versão (JaCoCo 0.8.11) — instrumentação não é variável de confusão. |
| Cobertura de linhas | 424/467 = 90,8% | 96,5% | — |
| Total de ramos no sistema | 42 | 64 | Denominadores pequenos dos dois lados; ver seção acima. |

A suíte do monólito tem 4 testes de integração com contexto Spring completo;
a versão em microsserviços, segundo o relatório dela, não tem nenhum — parte
da cobertura daqui vem desses testes, e essa assimetria é uma diferença real
entre as duas suítes, não só entre as arquiteturas.

## Metodologia

```bash
mvn clean test
# relatório navegável:   target/site/jacoco/index.html
# dados brutos (CSV):    target/site/jacoco/jacoco.csv
```

As colunas que importam no CSV: `BRANCH_COVERED`/`BRANCH_MISSED` para ramos
(a métrica oficial), `LINE_COVERED`/`LINE_MISSED` para linhas.
**`INSTRUCTION_COVERED`/`INSTRUCTION_MISSED` são outro contador — não chamar
de "ramos".**

Um snapshot do relatório HTML completo está versionado em
[`jacoco/index.html`](./jacoco/index.html). Relatório em Word com a análise
completa (incluindo a correção de metodologia em detalhe):
[`Relatorio-Cobertura-Codigo-JaCoCo.docx`](./Relatorio-Cobertura-Codigo-JaCoCo.docx).
