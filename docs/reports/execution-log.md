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

A suíte foi de 96 para 98 testes na rodada 4: dois casos novos em
`OrderStatusTransitionTest` (`confirmado_to_pendente_blocked`,
`confirmado_to_entregue_blocked`), fechando um gap encontrado por verificação
independente (ver seção de Mutation Score).

## Branch Coverage (JaCoCo) — corrigido

| # | Data/hora | Commit | Cobertura | Observação |
|---|---|---|---|---|
| ~~1~~ | ~~2026-08-13~~ | ~~`67d5a7b`~~ | ~~90,1% (1.785/1.982)~~ | **valor errado** — coluna de instruções lida como ramo |
| ~~2~~ | ~~2026-08-21~~ | ~~`3c46469`~~ | ~~90,1% (1.785/1.982)~~ | **valor errado** — mesma causa |
| 3 | 2026-08-25 | (verificação independente) | 78,6% (33/42) | valor correto, antes do fechamento do gap `OrderStatus` |
| 4 | 2026-08-28 | (após fechar o gap) | **81,0% (34/42)** | **valor de referência atual** |

Determinismo: verificado por hash MD5 do `jacoco.csv` em 3 execuções
consecutivas (idênticas byte a byte) além da comparação percentual — ver
`cobertura-codigo.md`.

## Mutation Score (PITest) — ambiente JDK 17 (declarado no projeto)

| # | Data/hora | Commit | Mortos | Score | Observação |
|---|---|---|---|---|---|
| 1 | 2026-08-13 | `67d5a7b` | 140/171 | 81,9% | baseline pós-refinamento |
| 2 | 2026-08-21 | `3c46469` | 140/171 | 81,9% | repetição, mesmo resultado |
| 3 | 2026-08-21 | `3c46469` | 140/171 | 81,9% | repetição, mesmo resultado |
| 4 | 2026-08-28 | (após fechar o gap `OrderStatus`) | **141/171** | **82,5%** | **valor de referência atual** — o mutante `OrderStatus$2.canTransitionTo` (encontrado por verificação independente) foi morto |

Uma verificação independente em ambiente JDK 21 obteve 167 mutantes totais
(não 171) — diferença de ambiente de compilação, documentada em
`mutation-score.md`. **JDK 17 é o valor de referência**, por ser a versão
declarada no `pom.xml`.

Determinismo: verificado mutante a mutante (não só o agregado) entre
execuções — mesmo conjunto de 171 mutantes, mesmo status para cada um, em
todas as rodadas.

## Violações Arquiteturais (ArchUnit)

| # | Data/hora | Regras | Violações |
|---|---|---|---|
| 1–4 | todas as rodadas acima | 7 | 0 |

Consistente em todas as execuções, incluindo a verificação independente.
0 violações no estado atual; 11 violações históricas foram encontradas e
corrigidas antes deste registro (ver `violacoes-arquiteturais.md`).

## Conclusão geral

As três métricas do protocolo GQM foram medidas de forma independente por
dois integrantes do grupo, em ambientes ligeiramente diferentes (JDK 17 vs.
JDK 21), com resultado consistente exceto pelo erro de coluna do CSV (já
corrigido) e pelo gap do `OrderStatus` (já fechado). Valores de referência
atuais, ambos determinísticos e reproduzíveis:

| Métrica | Valor |
|---|---|
| Testes | 98/98, 0 falhas |
| Branch Coverage | 81,0% (34/42) |
| Mutation Score | 82,5% (141/171) |
| Violações Arquiteturais | 0/7 regras |

## Como reproduzir

```bash
mvn test                                          # testes + JaCoCo
mvn org.pitest:pitest-maven:mutationCoverage      # PITest
```
