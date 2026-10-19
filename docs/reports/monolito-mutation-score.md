# Relatório de Mutation Score — pós-equalização (monólito)

**Ferramenta:** PITest 1.15.3 + `pitest-junit5-plugin` 1.2.1 (idêntico ao
estado anterior e à versão usada nos microsserviços)
**Sistema:** monolith-ecommerce
**Ambiente:** JDK 21.0.12.1 Temurin
**Branch:** `feature/equalizacao-testes-monolito`
**Critério medido:** fração de mutantes (alterações sintáticas mínimas no
bytecode) que a suíte mata ao falhar diante deles.

> **Correção de metodologia (reprodução independente, 2026-10-19):** a
> primeira versão deste relatório reportava 171/174 (98,3%). Esse número
> estava **contaminado por um build sujo**: o `target/classes` da máquina de
> desenvolvimento acumulava bytecode recompilado incrementalmente pela IDE
> entre uma sessão e outra, sem um `mvn clean` antes das medições. Como o
> total de mutantes depende só do bytecode de produção (nenhuma classe em
> `src/main` foi alterada nesta rodada), ele não deveria mudar — e a
> divergência (174 vs. o 170 histórico) foi o sinal de que havia uma
> variável de ambiente não controlada. Reproduzido do zero com
> `mvn clean test` / `mvn clean org.pitest:pitest-maven:mutationCoverage`,
> tanto numa worktree limpa do commit baseline (`9a389db`) quanto na branch
> desta equalização: o total correto é **170** mutantes em ambos os
> estados — idêntico ao baseline, como era de se esperar. Os números
> corrigidos abaixo substituem os publicados antes; os 3 sobreviventes
> residuais são exatamente os mesmos (mesma classe/método/linha/mutador),
> então nenhuma conclusão qualitativa muda — só o denominador.

## Resultado geral: Antes → Depois

| Status | Antes | Depois |
|---|---|---|
| `KILLED` | 137 (80,6%) | **167 (98,2%)** |
| `NO_COVERAGE` | 27 (15,9%) | **0 (0,0%)** |
| `SURVIVED` | 6 (3,5%) | **3 (1,8%)** |
| **Total gerado** | **170** | **170** |

O total de mutantes gerados **não muda** entre antes e depois — é esperado,
já que nenhuma classe de produção foi alterada; só o comportamento da suíte
de testes mudou. (Ver a nota de correção acima: a primeira versão deste
relatório reportava, por engano, 174.)

**Test Strength** (mortos ÷ alcançados — isola a qualidade das asserções do
que simplesmente foi "tocado" por algum teste): antes 137/143 = 95,8%;
depois **167/170 = 98,2%**. Com `NO_COVERAGE` zerado, test strength e
mutation score convergem para o mesmo número — não sobra mais nenhum
mutante "tocado mas não verificado".

## Mutation score por pacote

| Pacote | Mortos | Score |
|---|---|---|
| `exception` | 10/10 | 100,0% |
| `orders.controller` | 6/6 | 100,0% |
| `orders.dto` | 2/2 | 100,0% |
| `orders.entity` | 26/26 | 100,0% |
| `orders.service` | 17/17 | 100,0% |
| `products.controller` | 7/7 | 100,0% |
| `products.dto` | 1/1 | 100,0% |
| `products.entity` | 7/7 | 100,0% |
| `products.service` | 18/18 | 100,0% |
| `reports.controller` | 1/1 | 100,0% |
| `reports.service` | 1/1 | 100,0% |
| `users.controller` | 7/7 | 100,0% |
| `users.dto` | 2/2 | 100,0% |
| `users.entity` | 12/12 | 100,0% |
| `users.service` | 9/9 | 100,0% |
| `security` | 41/44 | 93,2% |
| **Total** | **167/170** | **98,2%** |

Todos os pacotes chegaram a 100% nesta rodada, exceto `security` — e os 3
pontos que faltam nele são exatamente os 3 residuais documentados abaixo,
não um gap de teste. Antes desta rodada, os pacotes mais fracos eram
`products.controller` (então inexistente como slice), `users.controller`,
`users.service` e `products.service` (ver `mutation-score.md` para os
números "Antes" completos) — eram precisamente os 27 `NO_COVERAGE`
concentrados ali.

## Os 3 mutantes SURVIVED remanescentes (cada um listado individualmente)

| Classe / método | Mutação | Por que sobrevive |
|---|---|---|
| `security.JwtService#isTokenExpired` (linha 99) | `BooleanFalseReturnValsMutator` — retorno sempre `false` | Inalcançável no fluxo normal: o parser do JJWT lança `ExpiredJwtException` para um token expirado antes de essa comparação ser avaliada — confirmado também como o único ramo de branch coverage em aberto (`monolito-cobertura-codigo.md`). |
| `security.ApplicationConfig#authenticationManager` | retorno trocado | Depende de `AuthenticationConfiguration` interno do Spring, só construído dentro de um `@SpringBootTest` completo — wiring de framework, fora do escopo de teste de unidade/slice por decisão do protocolo. |
| `security.SecurityConfig#securityFilterChain` | retorno trocado | Depende de `HttpSecurity` interno do Spring — mesma categoria do item acima. O efeito real deste bean (quais rotas exigem qual role) já é verificado indiretamente pelos 40 testes de slice de Controller, que falhariam se a cadeia de segurança estivesse errada. |

Nenhum dos três exige mudança em código de produção para morrer — matá-los
exigiria subir um `@SpringBootTest` completo, o que o protocolo restringe
(seção 1) justamente para não diluir a equalização por tipo de teste com
mais testes de contexto completo.

## Os 27 `NO_COVERAGE` da baseline — todos fechados

A tabela abaixo mapeia cada ponto da baseline para o teste que passou a
cobri-lo nesta rodada:

| Classe (baseline) | Método/linha | Teste que fechou |
|---|---|---|
| `ProductService` (7 mutantes) | `delete`, `searchByName`, `update` (×5) | `ProductServiceTest.deleteShould...`, `searchByNameShould...` (×2), `updateShould...` (×2 novos) |
| `UserService` (5 mutantes) | `delete`, `findAll`, `findById`, `lambda$0` (login) | `UserServiceTest.deleteShould...` (×2), `findAllShouldReturnMappedListOfUsers`, `findByIdShouldReturnMappedResponseWhenFound`, `loginShouldThrowIllegalStateWhenUserMissingAfterAuthentication` |
| `UserController` (5 mutantes) | `delete`, `findAll`, `findById`, `me` | `UserControllerTest` (slice novo, 8 testes cobrindo todos os endpoints) |
| `ProductController` (4 mutantes) | `delete`, `searchByName`, `update` | `ProductControllerTest` (slice novo, 11 testes) |
| `OrderService#findById` (1) | linha 81 | `OrderServiceTest.findByIdShouldReturnMappedResponseWhenFound` |
| `JwtService#generateToken` (1) | overload de 2 argumentos, linha 47 | `JwtServiceTest.generateTokenWithExtraClaimsShouldEmbedThemInToken` |
| `User#getCreatedAt` (1) | linha 90 | `UserTest.onCreateShouldPopulateCreatedAt` (invoca `onCreate()` diretamente, mesma package) |
| `OrderItem#getOrder` (1) | linha 58 | `OrderItemTest.gettersShouldReturnSnapshotValues` |
| `OrderController#findById` (1) | linha 59 | `OrderControllerTest.findByIdShouldReturnOrderWhenFound` |
| `UserResponse#from` (1) | linha 9 | Morto indiretamente por `UserServiceTest.findByIdShouldReturnMappedResponseWhenFound` (a asserção sobre os campos do DTO mapeado propaga para o `from`) |

27/27 fechados. Nenhum exigiu mudança em `src/main`.

## Determinismo da medição

PITest rodado **2 vezes** com `mvn clean` antes de cada rodada (a limpeza é
obrigatória — ver a nota de correção no topo desta página): ambas
produziram exatamente 167 `KILLED` / 3 `SURVIVED` / 0 `NO_COVERAGE`, com o
**mesmo conjunto de 3 sobreviventes, mutante a mutante** (mesma classe,
método, linha e mutador em `target/pit-reports/mutations.xml` nas duas
execuções). A mesma verificação foi repetida numa worktree Git separada,
a partir do commit baseline (`9a389db`), confirmando 170/170 mutantes
gerados ali — idêntico ao valor histórico documentado antes desta rodada.
Ver `execution-log.md`.

## Comparação com o "antes"

| Métrica | Antes | Depois |
|---|---|---|
| Mortos / Total | 137/170 | 167/170 |
| Mutation Score | 80,6% | **98,2%** |
| Test Strength | 95,8% | **98,2%** |
| `NO_COVERAGE` | 27 | **0** |
| `SURVIVED` | 6 | **3** (os 3 residuais de wiring/parser; os 3 outros do "antes" — `DatabaseSeeder` ×3 — foram mortos por `DatabaseSeederTest`, teste novo) |

## Metodologia

```bash
mvn clean org.pitest:pitest-maven:mutationCoverage
# o "clean" não é opcional — ver a nota de correção no topo desta página
# relatório navegável: target/pit-reports/index.html
# dados brutos:        target/pit-reports/mutations.xml
```
