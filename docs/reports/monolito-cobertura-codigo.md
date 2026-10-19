# Relatório de Cobertura de Código — pós-equalização (monólito)

**Ferramenta:** JaCoCo 0.8.11
**Sistema:** monolith-ecommerce
**Branch:** `feature/equalizacao-testes-monolito`
**Critério medido:** branch coverage (ramos) — a mesma métrica oficial do
protocolo, lida das colunas `BRANCH_COVERED`/`BRANCH_MISSED` do
`jacoco.csv`, nunca de `INSTRUCTION_COVERED`/`INSTRUCTION_MISSED` (ver a nota
de correção de metodologia em `cobertura-codigo.md`, que já documentou esse
erro uma vez — não repetido aqui).

## Resultado geral: Antes → Depois

| Estado | Ramos cobertos | Total | Branch Coverage |
|---|---|---|---|
| Antes (baseline, commit `9a389db`) | 34 | 42 | 81,0% |
| **Depois** | **41** | **42** | **97,6%** |

Ganho de **16,6 pontos percentuais**, fechando 7 dos 8 ramos que estavam em
aberto na baseline (seção "Ramos ainda sem cobertura" de `cobertura-codigo.md`).
O total de ramos no sistema não mudou (42) — nenhuma linha de produção foi
alterada, só testes novos passaram a exercitar ramos que já existiam.

## Branch coverage por pacote

| Pacote | Antes | Depois |
|---|---|---|
| `exception` | 2/2 = 100,0% | 2/2 = 100,0% |
| `orders.entity` | 12/12 = 100,0% | 12/12 = 100,0% |
| `orders.service` | 4/4 = 100,0% | 4/4 = 100,0% |
| `products.service` | 2/2 = 100,0% | 2/2 = 100,0% |
| `users.service` | 2/4 = 50,0% | **4/4 = 100,0%** |
| `security` | 12/18 = 66,7% | **17/18 = 94,4%** |
| **Total** | **34/42 = 81,0%** | **41/42 = 97,6%** |

`users.service` fechou os dois ramos abertos (checagem de existência em
`delete`, e o caminho "usuário sumiu entre autenticar e buscar" em `login`).
`security` fechou 5 dos 6 ramos abertos — os dois do `DatabaseSeeder`
(cenário "usuário já existe", nunca exercitado antes porque nenhum teste
desta classe existia) e os três do `JwtAuthenticationFilter` (header sem
prefixo `Bearer`, autenticação já presente no contexto, `extractUsername`
retornando `null` sem lançar exceção).

## O único ramo que continua em aberto

| Classe | Ramo em aberto | Por que fica assim |
|---|---|---|
| `security.JwtService#isTokenExpired` | 1 | Inalcançável no fluxo normal — o parser do JJWT lança `ExpiredJwtException` antes de essa comparação ser avaliada. Mesmo ponto reaparece como o único mutante residual desta classe em `monolito-mutation-score.md`. Tentar cobri-lo exigiria contornar o parser (ex.: construir o JWT manualmente sem passar pelo `Jwts.parserBuilder()`), o que testaria o teste, não o código. |

## Determinismo da medição

Verificado por 3 execuções consecutivas de `mvn test` nesta rodada — 180/180
testes idênticos nas três (ver `execution-log.md`, seção "Equalização..."),
e o `jacoco.csv` gerado reporta os mesmos 41/42 ramos em todas. Mesma
metodologia de verificação já usada no estado anterior (comparação byte a
byte do CSV).

## Comparação com o estado "Antes" (lido nas colunas corretas)

| Métrica | Antes | Depois |
|---|---|---|
| Branch coverage (ramos) | 34/42 = 81,0% | 41/42 = 97,6% |
| Cobertura de linhas | 407/450 = 90,4% | 448/450 = 99,6% |

> **Correção (reprodução independente, 2026-10-19):** a primeira versão
> deste relatório citava "424/467" para a cobertura de linhas do estado
> "Antes", copiado do relatório pré-existente sem remedir. Reexecutando o
> baseline (commit `9a389db`) numa worktree limpa, com `mvn clean test`, o
> valor correto é **407/450** — confirmado também pela medição "Depois"
> desta mesma rodada, que reporta o mesmo total de 450 linhas (produção não
> mudou). O branch coverage (34/42 → 41/42) não foi afetado por este erro.

Como já observado no relatório anterior, o sistema tem poucos ramos (42 no
total) concentrados em três pontos — máquina de estados do pedido, validação
de estoque e filtro JWT — então a métrica satura rápido uma vez que esses
pontos têm teste direcionado. É exatamente o que esta rodada fez: atacar os
ramos nomeados no protocolo (`JwtAuthenticationFilter`, `DatabaseSeeder`,
`UserService`, `JwtService`) em vez de perseguir o número agregado.

## Metodologia

```bash
mvn clean test
# relatório navegável:   target/site/jacoco/index.html
# dados brutos (CSV):    target/site/jacoco/jacoco.csv
```

Colunas relevantes: `BRANCH_COVERED`/`BRANCH_MISSED` (posições 7 e 6).
`INSTRUCTION_COVERED`/`INSTRUCTION_MISSED` são outro contador — não usar
como "cobertura de ramos".
