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

## Equalização da suíte de testes com a versão microsserviços (2026-10-19)

Trabalho feito em branch própria (`feature/equalizacao-testes-monolito`,
a partir do commit `9a389db`), seguindo o protocolo descrito em
`prompt-testes-monolito.md`: elevar o monólito ao mesmo critério de teste já
aplicado nos microsserviços (slices de Controller, reforço de Service,
paridade de regras ArchUnit), sem alterar código de produção. Os valores
"Antes" desta seção são o próprio estado de referência registrado acima
(rodada 8: 98 testes, 81,0% branch, 80,6% mutation, 0/7 ArchUnit).

### Testes (JUnit 5)

| # | Momento | Testes | Falhas | Erros | Resultado |
|---|---|---|---|---|---|
| 1 | Antes (baseline, = rodada 8 acima) | 98 | 0 | 0 | BUILD SUCCESS |
| 2 | Depois — 1ª execução | 180 | 0 | 0 | BUILD SUCCESS |
| 3 | Depois — 2ª execução | 180 | 0 | 0 | BUILD SUCCESS |
| 4 | Depois — 3ª execução | 180 | 0 | 0 | BUILD SUCCESS |

Determinismo: 180/180 idêntico nas três execuções consecutivas.

### Branch Coverage (JaCoCo)

| # | Momento | Cobertura | Observação |
|---|---|---|---|
| 1 | Antes | 81,0% (34/42) | baseline |
| 2 | Depois | **97,6% (41/42)** | só falta `JwtService#isTokenExpired` (ver `monolito-cobertura-codigo.md`) |

### Mutation Score (PITest)

| # | Momento | Ambiente | Mortos | Score | Observação |
|---|---|---|---|---|---|
| 1 | Antes (baseline) | JDK 21 | 137/170 | 80,6% | = rodada 8 acima |
| ~~2~~ | ~~Depois — 1ª rodada PITest~~ | ~~JDK 21~~ | ~~171/174~~ | ~~98,3%~~ | **errado** — `target/classes` sujo, ver correção abaixo |
| ~~3~~ | ~~Depois — 2ª rodada PITest~~ | ~~JDK 21~~ | ~~171/174~~ | ~~98,3%~~ | **errado**, mesma causa (idêntico mutante a mutante à rodada 2, o que mascarou o problema: parecia determinístico porque o build sujo era o mesmo nas duas) |
| 4 | Depois — 1ª rodada PITest, `mvn clean` antes | JDK 21 | **167/170** | **98,2%** | 0 NO_COVERAGE; 3 SURVIVED, todos documentados como fora de escopo |
| 5 | Depois — 2ª rodada PITest, `mvn clean` antes | JDK 21 | **167/170** | **98,2%** | idêntico mutante a mutante à rodada 4 (mesmos 3 sobreviventes, mesmo mutador cada um) |
| 6 | Verificação — baseline (`9a389db`) numa worktree Git limpa | JDK 21 | 137/170 | 80,6% | confirma que o total de mutantes do baseline é 170, não 174 — reproduzido do zero, sem depender do estado de `target/` da máquina de desenvolvimento |

> **Correção de metodologia (2026-10-19, mesmo dia):** as rodadas 2 e 3
> reportavam 174 mutantes gerados, 4 a mais que o baseline (170) — uma
> divergência que não deveria existir, já que nenhuma classe de produção
> foi alterada nesta equalização (o total de mutantes depende só do
> bytecode de produção). Investigado: o `target/classes` desta máquina
> acumulava bytecode recompilado incrementalmente por ferramentas da IDE
> entre sessões, sem `mvn clean` antes das medições de PITest — um build
> sujo. Confirmado rodando `mvn clean test` / `mvn clean
> org.pitest:pitest-maven:mutationCoverage` tanto na branch desta
> equalização quanto numa **worktree Git separada** a partir do commit
> baseline (rodada 6): o total correto nos dois estados é 170, consistente
> com o valor histórico. As rodadas 4 e 5 (com `clean`) são as válidas; as
> rodadas 2 e 3 ficam riscadas acima, não apagadas — eram reproduzíveis
> *dentro do ambiente contaminado*, então documentar o erro é mais
> informativo que escondê-lo. Os 3 mutantes `SURVIVED` são idênticos em
> todas as rodadas (contaminadas ou não) — o erro afetava só o
> denominador, nenhuma conclusão sobre quais mutantes sobrevivem muda.
> **Lição operacional:** a partir de agora, toda medição de PITest/JaCoCo
> neste projeto roda com `mvn clean` antes, nunca incremental.

Test strength (mortos ÷ alcançados, exclui NO_COVERAGE): 167/170 = 98,2%
(antes: 137/143 = 95,8%).

### Violações Arquiteturais (ArchUnit)

| # | Momento | Regras | Violações | Observação |
|---|---|---|---|---|
| 1 | Antes | 7 | 0 | — |
| ~~2~~ | ~~Depois (1ª versão)~~ | ~~10~~ | ~~0 ativas, 8 documentadas~~ | **metodologia questionada** — ver correção abaixo |
| 3 | Depois (corrigido) | **10** | **8** (5 + 3), pinadas por asserção, regras sem exceção de escopo | ver `monolito-violacoes-arquiteturais.md` |

> **Correção de metodologia (2026-10-19, revisão independente):** a
> primeira versão das 2 regras novas com violação real
> (`repositoriesShouldOnlyBeAccessedByServices`,
> `controllersShouldNotDependOnEntities`) embutia uma exceção de escopo no
> próprio predicado da regra (pacote `security` incluído na lista de
> acesso permitido; entidade `User` excluída do alvo), chegando a "0
> violações ativas em 10 regras". Uma revisão independente apontou que isso
> contraria a instrução de não afrouxar regra do ArchUnit para o teste
> passar, e torna o "0" do monólito incomparável com o "0" da versão em
> microsserviços — que aplica a mesma regra sem nenhuma exceção. **Corrigido**:
> as regras voltaram a ser estritas (idênticas à versão em microsserviços);
> os dois testes agora avaliam a regra manualmente
> (`ArchRule#evaluate(JavaClasses)`) e fixam o número exato de violações
> conhecidas (5 e 3) via `assertThat(...).hasSize(N)`, em vez de depender
> do runner `@ArchTest` padrão (que falharia a suíte em qualquer violação).
> A suíte continua verde porque o número documentado bate com o real, não
> porque a regra deixou de enxergar a violação. As 8 violações em si (quais
> classes, quantas ocorrências) não mudaram — só a forma como a suíte as
> representa. Ver `monolito-violacoes-arquiteturais.md` para o detalhe
> completo.

### Alterações em código de produção

Nenhuma. Toda a equalização veio de teste novo (entidade, service, slice de
controller, arquitetura) ou de teste existente reforçado.

## Como reproduzir

```bash
mvn clean test                                          # testes + JaCoCo — "clean" não é opcional, ver nota acima
mvn clean org.pitest:pitest-maven:mutationCoverage      # PITest
```
