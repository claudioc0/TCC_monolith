# Relatório de Mutation Score

**Ferramenta:** PITest 1.15.3 (+ `pitest-junit5-plugin`) — alinhado com a versão usada na medição em microsserviços
**Sistema:** monolith-ecommerce
**Ambiente:** JDK 21.0.12.1 Temurin (versão declarada no projeto — `<java.version>21</java.version>`)
**Critério medido:** fração de mutantes (alterações sintáticas mínimas e
deliberadas no bytecode — inverter uma condição, trocar uma constante, remover
uma chamada) que a suíte de testes detecta (mata) ao falhar diante deles.

> **Nota de migração (2026-09-11):** por pedido do orientador, o projeto
> padronizou em **JDK 21** (a mesma versão que o Eric já usava na verificação
> independente), eliminando o JDK como variável de ambiente divergente entre
> as medições. Os números abaixo são a nova referência única; os valores
> anteriores em JDK 17 (141/174 = 81,0%) ficam registrados como histórico em
> `execution-log.md` e `replicacao-e-determinismo.md`.

## Resultado geral

| Status | Mutantes | % |
|---|---|---|
| `KILLED` (detectado por algum teste) | 137 | 80,6% |
| `NO_COVERAGE` (código nunca alcançado por nenhum teste) | 27 | 15,9% |
| `SURVIVED` (alcançado, mas nenhum teste falhou) | 6 | 3,5% |
| **Total gerado** | **170** | — |

**Test Strength** (mortos / alcançados = 137/143): **95,8%**.

4 mutantes a menos que em JDK 17 (174 → 170) — o mesmo padrão já documentado
antes da migração (compiladores de JDKs diferentes geram bytecode
ligeiramente distinto para a mesma classe fonte). Nenhum dos 6 sobreviventes
mudou; só o total de mutantes gerados em `orders.entity` (-3) e `security`
(-1) foi afetado. Ver seção "De onde vêm os 4 mutantes a menos".

## Mutation score por pacote

| Pacote | Mortos | Score |
|---|---|---|
| `exception` | 10/10 | 100,0% |
| `orders.dto` | 2/2 | 100,0% |
| `products.dto` | 1/1 | 100,0% |
| `products.entity` | 7/7 | 100,0% |
| `reports.controller` | 1/1 | 100,0% |
| `reports.service` | 1/1 | 100,0% |
| `orders.entity` | 25/26 | 96,2% |
| `orders.service` | 16/17 | 94,1% |
| `users.entity` | 11/12 | 91,7% |
| `security` | 37/44 | 84,1% |
| `orders.controller` | 5/6 | 83,3% |
| `products.service` | 11/18 | 61,1% |
| `users.dto` | 1/2 | 50,0% |
| `users.service` | 4/9 | 44,4% |
| `products.controller` | 3/7 | 42,9% |
| `users.controller` | 2/7 | 28,6% |
| **Total** | **137/170** | **80,6%** |

## Mutantes SURVIVED — os 6 atuais

| Classe / método | Mutação | Por que sobrevive |
|---|---|---|
| `JwtService#isTokenExpired` | boolean sempre `false` | Inalcançável no fluxo normal: o parser do JJWT lança `ExpiredJwtException` para um token expirado antes de essa comparação ser avaliada. |
| `ApplicationConfig#authenticationManager` | retorno `null` | Depende de `AuthenticationConfiguration` interno do Spring, só construído dentro de um `@SpringBootTest` completo. |
| `SecurityConfig#securityFilterChain` | retorno `null` | Depende de `HttpSecurity` interno do Spring — mesma categoria. |
| `DatabaseSeeder#seedAdmin`, linha 17 | condicional negado (`isEmpty()` do e-mail do admin) | Nos testes o banco sempre começa vazio, então esse `if` só é exercitado pelo lado "não existe" — nenhum teste recria o cenário "usuário já existe" para checar que o seeder não duplica. |
| `DatabaseSeeder#seedAdmin`, linha 27 | condicional negado (`isEmpty()` do e-mail do cliente) | Mesma causa da linha anterior, para o segundo usuário semeado. |
| `DatabaseSeeder#seedAdmin`, linha 16 | retorno do bean `CommandLineRunner` trocado por `null` | Nenhum teste inspeciona o bean em si, só o efeito colateral (usuários no banco) — e esse efeito nunca é checado no cenário "banco não vazio" descrito acima. |

Exatamente o mesmo conjunto de 6 sobreviventes de antes da migração (mesma
classe, método e linha) — a mudança de JDK não abriu nem fechou nenhum gap
de teste, só mudou o total de mutantes gerados (seção abaixo). As 3
primeiras são código de *wiring* de framework — categoria classicamente
aceita como residual em mutation testing. As 3 últimas são teste faltando de
verdade, mas de baixo valor prático: exigiria popular o banco antes do
seeder rodar só para verificar que ele não duplica um usuário, um cenário de
bootstrap fora do fluxo de negócio medido pelo protocolo. Registrado como
residual aceito.

## Um quarto sobrevivente que existiu e foi corrigido: o caso `OrderStatus`

Uma verificação independente feita por um integrante do grupo (Eric), rodando
a mesma suíte, encontrou um **quarto mutante sobrevivente** que não estava
nesta lista: `OrderStatus$2.canTransitionTo` (a constante `CONFIRMADO`),
mutador `BooleanTrueReturnValsMutator`, linha 22. Trocar o retorno por `true`
fazia o pedido aceitar **qualquer** transição a partir de `CONFIRMADO` sem que
nenhum teste reclamasse — a suíte testava as duas transições permitidas
(`CONFIRMADO→ENVIADO`, `CONFIRMADO→CANCELADO`) mas nunca uma bloqueada
diretamente a partir desse estado.

Diferente dos outros três, este **não é código de wiring nem inalcançável** —
é teste faltando de verdade, e barato de corrigir. Foi fechado adicionando
dois casos em `OrderStatusTransitionTest`:
`confirmado_to_pendente_blocked` e `confirmado_to_entregue_blocked`. Depois da
correção, o mutante morre — a tabela e os números desta página já incorporam
essa correção (é anterior, e independente, da mudança de escopo e da
migração de JDK descritas abaixo).

## Os 27 mutantes sem cobertura

Concentrados em métodos de controller e nos serviços que só esses controllers
chamam — o padrão estrutural é o mesmo em toda a suíte:

| Classe | Métodos não exercitados |
|---|---|
| `ProductService` | `delete`, `searchByName`, `update` |
| `UserService` | `delete`, `findAll`, `findById` e mais |
| `UserController` | `delete`, `findAll`, `findById`, `me` |
| `ProductController` | `delete`, `searchByName`, `update` |

A causa é estrutural, não asserção fraca: a suíte não tem teste de fatia web
(`@WebMvcTest`) nem `UserIntegrationTest`. Os controllers só são exercitados
de passagem pelos quatro testes de integração existentes, que cobrem os
fluxos principais (criar pedido, relatório, registro/login) mas não
`update`/`delete`/`search`/listagens administrativas. Nenhum desses buracos
precisa de mudança em código de produção — é teste faltando.

## De onde vêm os 4 mutantes a menos (174 → 170)

Comparando mutante a mutante contra a rodada em JDK 17: `orders.entity` gerou
3 mutantes a menos (29 → 26) e `security` gerou 1 a menos (45 → 44); todos os
outros pacotes ficaram idênticos. Nenhum mutante "sumiu" de uma classe que
antes tinha `SURVIVED` — a diferença está inteira em código já 100%/quase
100% morto, então não muda nenhuma conclusão qualitativa. É o mesmo
fenômeno de bytecode já documentado antes da migração (tabelas de switch de
enum, métodos sintéticos de lambda) — só que agora não é mais uma
comparação *entre* dois ambientes diferentes, é a diferença que a própria
migração de JDK 17 para 21 causou neste projeto, medida diretamente.

## Ambiente: por que a padronização em JDK 21 aconteceu

Antes desta migração, o monólito declarava JDK 17 enquanto a verificação
independente do Eric usava JDK 21 — uma divergência de ambiente documentada
como ameaça à validade (ver `replicacao-e-determinismo.md`, seção 2.2). Por
pedido do orientador, o projeto passou a declarar e usar JDK 21 também,
eliminando essa variável em vez de só documentá-la. Não houve necessidade de
decidir "qual dos dois é a referência" — a decisão foi convergir para o
mesmo ambiente que o outro lado já usava.

> **Pendência de escopo (fora deste repositório):** a padronização em JDK 21
> precisa ser declarada formalmente também no `pom.xml` de microsserviços
> (o Eric já *usa* 21 ao rodar, mas o projeto de lá pode não *declarar* essa
> versão) — registrado aqui como pendência de alinhamento entre os dois
> lados, não uma tarefa deste repositório.

## Uma ameaça resolvida, outra fechada nesta rodada

1. ~~**Versão do PITest**: monólito usa 1.15.8, microsserviços usa 1.15.3.~~
   **Resolvido** — o monólito passou a usar 1.15.3 também.
2. ~~**Classes excluídas do escopo**: o monólito excluía `MonolithApplication`,
   `DatabaseSeeder` e o pacote `architecture`; a versão em microsserviços não
   tinha exclusão equivalente documentada.~~ **Resolvido** — a exclusão foi
   removida do `pom.xml`. Medido o efeito antes de decidir: dos três itens
   excluídos, só `DatabaseSeeder` tinha mutante de verdade (3 novos, todos
   `SURVIVED`; `MonolithApplication` e `architecture.*` geram zero mutante
   cada). A suspeita registrada — "excluir código de bootstrap tende a
   favorecer o lado que exclui" — se confirmou: o escore caiu de 82,5% para
   81,0% (medido em JDK 17, na época), o número real e não mais inflado.
   Mantida apenas a exclusão de `architecture.*` das *classes de teste*
   rodadas contra cada mutante (`excludedTestClasses`) — é otimização de
   tempo de execução, não de escopo: testes ArchUnit verificam dependência
   estrutural entre classes, não comportamento em tempo de execução, então
   não matam mutante nenhum de qualquer forma.
3. ~~**Divergência de JDK**: monólito declarava 17, verificação do Eric usava
   21.~~ **Resolvido (2026-09-11)** — projeto migrado para JDK 21 por pedido
   do orientador. Ver seção "Ambiente" acima.

## Determinismo da medição

Verificado de duas formas: comparação de totais entre execuções (ver
`execution-log.md`) e, de forma mais rigorosa, **comparação mutante a
mutante** entre rodadas (mesma classe, método, linha, mutador e status final
para cada um dos 170 — não só o agregado). As rodadas produziram exatamente o
mesmo conjunto em todas as vezes, tanto em JDK 17 quanto em JDK 21
(separadamente).

## Metodologia

```bash
mvn org.pitest:pitest-maven:mutationCoverage
# relatório navegável, com cada mutante e o teste que o matou:
target/pit-reports/index.html
# dados brutos usados nesta página:
target/pit-reports/mutations.xml
```

Um snapshot do relatório HTML completo está versionado em
[`pitest/index.html`](./pitest/index.html). Relatório em Word com a análise
completa da rodada anterior em JDK 17 (incluindo o achado do `OrderStatus` e
a comparação de ambiente que motivou esta migração):
[`Relatorio-Mutation-Score-PITest.docx`](./Relatorio-Mutation-Score-PITest.docx).
