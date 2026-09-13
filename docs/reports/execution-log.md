# Log de Execuções da Suíte de Testes

Registro acumulado de execuções das três métricas de teste do protocolo GQM,
para evidenciar estabilidade/determinismo — nenhuma execução deve apresentar
resultado diferente das outras, a variável de ambiente sendo controlada.

> **Nota de correção:** as entradas de Branch Coverage abaixo, marcadas como
> "90,1% (1.785/1.982)" em execuções anteriores, usavam a coluna errada do
> CSV do JaCoCo (instruções de bytecode, não ramos). O valor correto está
> corrigido nas linhas mais recentes e detalhado em `cobertura-codigo.md`.
> Erro encontrado por verificação independente de um segundo integrante do
> grupo (Eric).

## Testes (JUnit 5: unitários + integração + arquitetura)

| # | Data/hora | Commit | Testes | Falhas | Erros | Duração | Resultado |
|---|---|---|---|---|---|---|---|
| 1 | 2026-08-13 | `67d5a7b` | 96 | 0 | 0 | não registrada | BUILD SUCCESS |
| 2 | 2026-08-21 19:21 | `3c46469` | 96 | 0 | 0 | 32,8s | BUILD SUCCESS |
| 3 | 2026-08-21 19:22 | `3c46469` | 96 | 0 | 0 | 30,0s | BUILD SUCCESS |
| 4 | 2026-08-28 | (fechamento do gap `OrderStatus`) | 98 | 0 | 0 | — | BUILD SUCCESS |
| 5 | 2026-09-11 | (migração para JDK 21) | 98 | 0 | 0 | — | BUILD SUCCESS |

A suíte foi de 96 para 98 testes na rodada 4: dois casos novos em
`OrderStatusTransitionTest` (`confirmado_to_pendente_blocked`,
`confirmado_to_entregue_blocked`), fechando um gap encontrado por verificação
independente (ver seção de Mutation Score). Contagem de testes idêntica
depois da migração de JDK (rodada 5) — só as métricas medidas em bytecode
(Mutation Score) foram sensíveis à mudança de compilador.

## Branch Coverage (JaCoCo) — corrigido

| # | Data/hora | Commit | Cobertura | Observação |
|---|---|---|---|---|
| ~~1~~ | ~~2026-08-13~~ | ~~`67d5a7b`~~ | ~~90,1% (1.785/1.982)~~ | **valor errado** — coluna de instruções lida como ramo |
| ~~2~~ | ~~2026-08-21~~ | ~~`3c46469`~~ | ~~90,1% (1.785/1.982)~~ | **valor errado** — mesma causa |
| 3 | 2026-08-25 | (verificação independente) | 78,6% (33/42) | valor correto, antes do fechamento do gap `OrderStatus` |
| 4 | 2026-08-28 | (após fechar o gap) | 81,0% (34/42) | valor de referência em JDK 17 |
| 5 | 2026-09-11 | (migração para JDK 21) | **81,0% (34/42)** | **valor de referência atual** — idêntico ao JDK 17; branch coverage não é sensível a diferença de bytecode entre JDKs neste projeto |

Determinismo: verificado por hash MD5 do `jacoco.csv` em 3 execuções
consecutivas (idênticas byte a byte) além da comparação percentual — ver
`cobertura-codigo.md`.

## Mutation Score (PITest)

| # | Data/hora | Commit | Ambiente | Mortos | Score | Observação |
|---|---|---|---|---|---|---|
| 1 | 2026-08-13 | `67d5a7b` | JDK 17 | 140/171 | 81,9% | baseline pós-refinamento |
| 2 | 2026-08-21 | `3c46469` | JDK 17 | 140/171 | 81,9% | repetição, mesmo resultado |
| 3 | 2026-08-21 | `3c46469` | JDK 17 | 140/171 | 81,9% | repetição, mesmo resultado |
| 4 | 2026-08-28 | (após fechar o gap `OrderStatus`) | JDK 17 | 141/171 | 82,5% | mutante `OrderStatus$2.canTransitionTo` (encontrado por verificação independente) foi morto |
| 5 | 2026-09-04 | (PITest 1.15.8 → 1.15.3, alinhado com microsserviços) | JDK 17 | 141/171 | 82,5% | resultado idêntico à rodada 4, mutante a mutante; confirma que a versão do PITest não era a causa da divergência de ambiente (era o JDK) |
| 6 | 2026-09-04 | (exclusão de `MonolithApplication`/`DatabaseSeeder`/`architecture.*` removida do `pom.xml`) | JDK 17 | 141/174 | 81,0% | 3 mutantes novos, todos em `DatabaseSeeder`, todos `SURVIVED`; confirma que a exclusão inflava o score em 1,5 p.p.; repetido 2×, resultado idêntico mutante a mutante nas duas |
| 7 | 2026-09-11 | (migração para JDK 21, pedido do orientador) | JDK 21 | 137/170 | 80,6% | 4 mutantes a menos que em JDK 17 (bytecode diferente em `orders.entity` e `security`), mesmos 6 sobreviventes de antes; elimina a divergência de ambiente com a verificação do Eric |
| 8 | 2026-09-12 | `1c05487` (terceira máquina, integrante Vinicius) | JDK 21 | **137/170** | **80,6%** | **valor de referência atual** — verificação independente em máquina separada; idêntico mutante a mutante à rodada 7, incluindo o tipo de mutador de cada um dos 6 sobreviventes (checado individualmente, não só o agregado) |

A rodada 7 substitui a comparação "JDK 17 (referência) vs. JDK 21 (Eric)"
que existia antes — agora os dois lados usam o mesmo ambiente, então deixou
de ser uma ameaça à validade e passou a ser só um dado histórico (ver
`replicacao-e-determinismo.md`, seção 2.2). A rodada 8 é a terceira
confirmação independente do mesmo resultado (própria, Eric originalmente,
e agora Vinicius em JDK 21) — a primeira tentativa dele (registrada à parte)
reproduziu por engano o estado *anterior* à rodada 6 (171/141, 3
sobreviventes) por não ter recompilado do zero depois de mudar o
`<java.version>` manualmente; corrigido com `git pull` + `mvnw clean test`
antes de repetir.

Determinismo: verificado mutante a mutante (não só o agregado) entre
execuções — mesmo conjunto de mutantes, mesmo status para cada um, em todas
as rodadas de um mesmo ambiente/configuração.

## Violações Arquiteturais (ArchUnit)

| # | Data/hora | Regras | Violações |
|---|---|---|---|
| 1–5 | todas as rodadas acima, incluindo JDK 21 | 7 | 0 |

Consistente em todas as execuções, incluindo a verificação independente e a
migração de JDK. 0 violações no estado atual; 11 violações históricas foram
encontradas e corrigidas antes deste registro (ver `violacoes-arquiteturais.md`).

## Conclusão geral

As três métricas do protocolo GQM foram medidas de forma independente por
dois integrantes do grupo — inicialmente em ambientes ligeiramente
diferentes (JDK 17 vs. JDK 21), depois padronizadas em JDK 21 por pedido do
orientador (2026-09-11) — com resultado consistente exceto pelo erro de
coluna do CSV (já corrigido) e pelo gap do `OrderStatus` (já fechado).
Valores de referência atuais, todos determinísticos e reproduzíveis, em
JDK 21:

| Métrica | Valor |
|---|---|
| Testes | 98/98, 0 falhas |
| Branch Coverage | 81,0% (34/42) — 73,8% (31/42) só com testes de unidade |
| Mutation Score | 80,6% (137/170) |
| Violações Arquiteturais | 0/7 regras |

Ver [`resultados-finais.md`](./resultados-finais.md) para a linhagem
completa de cada número (valor errado → correção → valor final) e o estado
das ameaças à validade.

## Como reproduzir

```bash
mvn test                                          # testes + JaCoCo
mvn org.pitest:pitest-maven:mutationCoverage      # PITest
```
