# Relatório de Violações Arquiteturais — pós-equalização (monólito)

**Ferramenta:** ArchUnit 1.2.1 (`archunit-junit5`)
**Sistema:** monolith-ecommerce
**Branch:** `feature/equalizacao-testes-monolito`
**Critério medido:** densidade de violações — dependências reais no bytecode
compilado que contradizem regras de arquitetura declaradas explicitamente.

> **Correção de metodologia (reprodução independente, 2026-10-19):** a
> primeira versão deste relatório embutia, nas duas regras novas com
> violação real, uma exceção de escopo (predicado que excluía `User` do
> alvo de `controllersShouldNotDependOnEntities`, e ampliação da lista de
> pacotes permitidos em `repositoriesShouldOnlyBeAccessedByServices` para
> incluir `..security..`) — chegando a "0 violações ativas em 10 regras".
> Isso foi sinalizado, corretamente, como uma forma de afrouxar a regra
> para o teste passar: as duas regras novas existem justamente *porque* a
> versão em microsserviços as aplica sem nenhuma exceção, e relaxá-las aqui
> torna o "0" do monólito incomparável com o "0" de lá — um não mede a
> mesma coisa que o outro. **Corrigido**: as regras voltaram a ser
> estritas, idênticas à versão usada nos microsserviços (nenhum pacote ou
> classe excluído do predicado); em vez de depender do runner padrão do
> ArchUnit (que falha a suíte em qualquer violação), os dois testes
> avaliam a regra manualmente (`ArchRule#evaluate`) e **fixam o número
> exato de violações conhecidas** via asserção — a suíte continua verde,
> mas porque o número documentado bate com o real, não porque a regra foi
> afrouxada. Se uma violação nova aparecer ou uma existente for corrigida,
> o teste quebra, forçando atualizar este relatório conscientemente. Os
> números de violações abaixo (5 e 3) não mudaram com a correção — só a
> forma como a suíte os representa.

## Resultado: suíte × regras × violações

| Suíte | Regras | Violações | Como a suíte fica verde |
|---|---|---|---|
| `CommonArchitectureRulesTest` | 3 | 0 | Regra realmente sem violação (`@ArchTest` padrão) |
| `DomainBoundaryArchitectureTest` | 4 | 0 | Regra realmente sem violação (`@ArchTest` padrão, com exclusão documentada da camada de controller — ver nota de escopo já existente antes desta rodada) |
| `ParityArchitectureRulesTest` (nova, paridade com microsserviços) | 3 | **8** (5 + 3) | 1 regra sem violação; 2 regras estritas com violação real, cujo número exato (5 e 3) é **fixado por asserção**, não zerado |
| **Total** | **10** | **8** | — |

Diferente das regras 4–7 (que excluem a camada de controller do próprio
predicado, uma decisão de escopo documentada desde antes desta rodada), as
duas regras novas com violação **não têm nenhuma exceção no predicado**: o
predicado é idêntico ao da versão em microsserviços. A suíte fica verde
porque o teste pina o número conhecido de violações, não porque a regra foi
redesenhada para não vê-las.

## As 10 regras (o que cada uma verifica)

### Regras comuns (`CommonArchitectureRulesTest`) — inalteradas

| # | Regra | O que verifica |
|---|---|---|
| 1 | `controllers_should_not_access_repositories_directly` | Nenhuma classe em `..controller..` depende de classes em `..repository..`. |
| 2 | `services_should_not_depend_on_controllers` | Nenhuma classe em `..service..` depende de classes em `..controller..`. |
| 3 | `controller_endpoints_should_not_return_entities` | Nenhum método público de Controller **retorna** uma entidade `@Entity` (checagem pelo tipo genérico real de retorno). |

### Regras de fronteira entre domínios (`DomainBoundaryArchitectureTest`) — inalteradas

| # | Regra | O que verifica |
|---|---|---|
| 4–7 | `{orders,products,users,reports}_should_not_access_other_domains_internals` | Camadas service/repository de cada domínio não acessam `entity`/`repository` de outro domínio diretamente. |

### Regras novas de paridade com microsserviços (`ParityArchitectureRulesTest`)

| # | Regra | O que verifica | Violações reais | Como fica verde |
|---|---|---|---|---|
| 8 | `namingConventionShouldBeRespected` | `@RestController` termina em `Controller`, `@Service` em `Service`, `@Repository` em `Repository`. | **0** | Sem violação — `assertThat(violations).isEmpty()` |
| 9 | `repositoriesShouldOnlyBeAccessedByServices` | Classes em `..repository..` só são acessadas de `..repository..`/`..service..` — **sem exceção de pacote**. | **3** | `assertThat(violations).hasSize(3)` |
| 10 | `controllersShouldNotDependOnEntities` | Nenhuma classe `@RestController` **depende** de classe `@Entity` — **sem exceção de classe** (mais forte que a regra 3: checa dependência, não só tipo de retorno). | **5** | `assertThat(violations).hasSize(5)` |

## As 8 violações encontradas (quantas, em que classes, qual decisão)

### `controllersShouldNotDependOnEntities` — 5 ocorrências

| Classe.Método | Dependência |
|---|---|
| `OrderController.create` | parâmetro `@AuthenticationPrincipal User currentUser` + chamada a `User.getId()` |
| `OrderController.myOrders` | parâmetro `@AuthenticationPrincipal User currentUser` + chamada a `User.getId()` |
| `UserController.me` | parâmetro `@AuthenticationPrincipal User currentUser` |

(3 classes/métodos, 5 arestas de dependência — duas delas contam duas vezes
porque o ArchUnit reporta separadamente "tem parâmetro do tipo X" e "chama
método de X".)

**Decisão:** documentar, não corrigir. `@AuthenticationPrincipal User` é como
o Spring Security injeta o principal autenticado — é assim que o
`JwtAuthenticationFilter` evita que o cliente passe o próprio `userId` no
corpo da requisição (ver o teste que fixa esse contrato em
`monolito-testes-unitarios.md`). Trocar por um DTO exigiria reimplementar
`UserDetails` ou mapear o principal manualmente em cada endpoint — mudança
de produção maior que o problema que resolve, e motivada só por fazer um
teste de arquitetura passar. A regra em si **não foi relaxada**: continua
verificando qualquer classe `@RestController` contra qualquer classe
`@Entity`, sem exclusão nenhuma; o teste apenas fixa que, hoje, há
exatamente 5 ocorrências, todas nesses 3 pontos.

### `repositoriesShouldOnlyBeAccessedByServices` — 3 ocorrências

| Classe.Método | Acesso |
|---|---|
| `ApplicationConfig.userDetailsService` (lambda) | `UserRepository.findByEmail` |
| `DatabaseSeeder.seedAdmin` (lambda, linha 17) | `UserRepository.findByEmail` |
| `DatabaseSeeder.seedAdmin` (lambda, linha 27) | `UserRepository.findByEmail` |

**Decisão:** documentar, não corrigir. É wiring de autenticação — o
`UserDetailsService` do Spring Security precisa do repositório na própria
definição do bean — e seed de dados de inicialização, não acoplamento de
regra de negócio entre domínios. A regra **não foi relaxada**: continua
exigindo que repositórios só sejam acessados por `..repository..`/`..service..`,
sem incluir `..security..` na lista de pacotes permitidos; o teste fixa que,
hoje, há exatamente 3 ocorrências, todas nesses 2 pontos.

### `namingConventionShouldBeRespected` — 0 ocorrências

Todas as classes anotadas já seguem a convenção — nenhuma decisão de escopo
precisou ser tomada para esta regra.

## Alterações em código de produção

**Nenhuma.** As 8 violações ficam registradas como dado do experimento, não
corrigidas — nem a regra, nem o código de produção foram alterados para
fazê-las desaparecer.

## Por que essa comparação importa (num monólito vs. num microsserviço)

Num monólito, nada no compilador ou no runtime impede uma classe de importar
qualquer outra classe do mesmo artefato — a fronteira entre camadas e entre
domínios existe só por convenção, e só o ArchUnit a torna visível e
mensurável. Numa decomposição em microsserviços, a mesma fronteira passa a
ser imposta fisicamente: sem compartilhamento de classpath entre serviços, o
acoplamento indevido deixa de ser uma violação de estilo e se torna
literalmente impossível de compilar. **A densidade de violações no monólito
é a medida direta do que a fronteira física do microsserviço substitui** —
as 8 ocorrências encontradas nesta rodada (5 de injeção de principal
autenticado, 3 de wiring de autenticação) são exatamente o tipo de
acoplamento que, no lado de microsserviços, nem chegaria a existir como
possibilidade: `User` e `UserRepository` estão em processos/deployables
diferentes dos controllers que os consumiriam indevidamente. Essa
comparação só é válida porque as regras aqui são estritas — um "0" obtido
afrouxando a regra teria escondido justamente o dado que a comparação
precisa mostrar.

## Metodologia

```bash
mvn test
# as 3 classes de arquitetura aparecem no Surefire como qualquer outra classe de teste
```

Código-fonte: [`ParityArchitectureRulesTest.java`](../../src/test/java/com/pucpr/tcc/monolith/architecture/ParityArchitectureRulesTest.java) —
usa `ArchRule#evaluate(JavaClasses)` + `EvaluationResult.getFailureReport().getDetails()`
para as duas regras com violação conhecida, em vez do runner `@ArchTest`
padrão (que trataria qualquer violação como falha de suíte).
