# Relatório de Mutation Score

**Ferramenta:** PITest 1.15.3 (+ `pitest-junit5-plugin`) — alinhado com a versão usada na medição em microsserviços
**Sistema:** monolith-ecommerce
**Ambiente:** JDK 17.0.18 (versão declarada no projeto — `<java.version>17</java.version>`)
**Critério medido:** fração de mutantes (alterações sintáticas mínimas e
deliberadas no bytecode — inverter uma condição, trocar uma constante, remover
uma chamada) que a suíte de testes detecta (mata) ao falhar diante deles.

## Resultado geral

| Status | Mutantes | % |
|---|---|---|
| `KILLED` (detectado por algum teste) | 141 | 82,5% |
| `NO_COVERAGE` (código nunca alcançado por nenhum teste) | 27 | 15,8% |
| `SURVIVED` (alcançado, mas nenhum teste falhou) | 3 | 1,8% |
| **Total gerado** | **171** | — |

**Test Strength** (mortos / alcançados = 141/144): **97,9%**.

## Mutation score por pacote

| Pacote | Mortos | Score |
|---|---|---|
| `exception` | 10/10 | 100,0% |
| `orders.dto` | 2/2 | 100,0% |
| `products.dto` | 1/1 | 100,0% |
| `products.entity` | 7/7 | 100,0% |
| `reports.controller` | 1/1 | 100,0% |
| `reports.service` | 1/1 | 100,0% |
| `orders.entity` | 28/29 | 96,6% |
| `orders.service` | 16/17 | 94,1% |
| `users.entity` | 11/12 | 91,7% |
| `security` | 38/42 | 90,5% |
| `orders.controller` | 5/6 | 83,3% |
| `products.service` | 11/18 | 61,1% |
| `users.dto` | 1/2 | 50,0% |
| `users.service` | 4/9 | 44,4% |
| `products.controller` | 3/7 | 42,9% |
| `users.controller` | 2/7 | 28,6% |
| **Total** | **141/171** | **82,5%** |

## Mutantes SURVIVED — os 3 que restam

| Classe / método | Mutação | Por que sobrevive |
|---|---|---|
| `JwtService#isTokenExpired` | boolean sempre `false` | Inalcançável no fluxo normal: o parser do JJWT lança `ExpiredJwtException` para um token expirado antes de essa comparação ser avaliada. |
| `ApplicationConfig#authenticationManager` | retorno `null` | Depende de `AuthenticationConfiguration` interno do Spring, só construído dentro de um `@SpringBootTest` completo. |
| `SecurityConfig#securityFilterChain` | retorno `null` | Depende de `HttpSecurity` interno do Spring — mesma categoria. |

Código de *wiring* de framework — categoria classicamente aceita como residual
em mutation testing.

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
correção, o mutante morre e o mutation score sobe de 141/171 (82,5% já
refletido acima) — a tabela e os números desta página já incorporam essa
correção.

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

## Ambiente: por que o JDK declarado importa

A mesma verificação independente rodou em JDK 21.0.9 e obteve um total
**diferente** de mutantes gerados (167, contra 171 aqui) — o compilador do
JDK 21 produz bytecode ligeiramente distinto do JDK 17, e menos instrução às
vezes significa menos alvo de mutação. Isso não é erro de nenhum dos dois
lados; é variável de ambiente que precisa estar declarada ao reportar o
número. **171 é o valor de referência**, porque `<java.version>` do projeto
declara 17, e é o ambiente usado nesta página. O relatório em Word do outro
integrante documenta essa diferença em detalhe e recomenda o mesmo critério.

## Uma diferença já resolvida, uma ainda em aberto

1. ~~**Versão do PITest**: monólito usa 1.15.8, microsserviços usa 1.15.3.~~
   **Resolvido** — o monólito passou a usar 1.15.3 também. Resultado após o
   downgrade: **idêntico**, mutante a mutante (141/171 mortos, mesmos 3
   sobreviventes) — a mudança de versão não alterou nada neste projeto. A
   diferença de versão em si nunca foi a causa da divergência de ambiente
   registrada na seção anterior; era o JDK (17 vs. 21), não o PITest.
2. **Classes excluídas do escopo** — ainda em aberto: o monólito exclui
   `MonolithApplication`, `DatabaseSeeder` e o pacote `architecture`; a
   versão em microsserviços não tem exclusão equivalente documentada.
   Excluir código de bootstrap/seed tende a favorecer o lado que exclui,
   porque é justamente o tipo de código que concentra mutante difícil de
   matar. Ameaça à validade ainda não resolvida.

## Determinismo da medição

Verificado de duas formas: comparação de totais entre execuções (ver
`execution-log.md`) e, de forma mais rigorosa, **comparação mutante a
mutante** entre rodadas (mesma classe, método, linha, mutador e status final
para cada um dos 171 — não só o agregado). As rodadas produziram exatamente o
mesmo conjunto em todas as vezes.

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
completa (incluindo o achado do `OrderStatus` e a comparação de ambiente):
[`Relatorio-Mutation-Score-PITest.docx`](./Relatorio-Mutation-Score-PITest.docx).
