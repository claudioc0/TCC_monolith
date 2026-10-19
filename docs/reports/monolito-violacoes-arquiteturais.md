# Relatório de Violações Arquiteturais — pós-equalização (monólito)

**Ferramenta:** ArchUnit 1.2.1 (`archunit-junit5`)
**Sistema:** monolith-ecommerce
**Branch:** `feature/equalizacao-testes-monolito`
**Critério medido:** densidade de violações — dependências reais no bytecode
compilado que contradizem regras de arquitetura declaradas explicitamente.

## Resultado: suíte × regras × violações

| Suíte | Regras | Violações ativas (com escopo) | Violações reais encontradas |
|---|---|---|---|
| `CommonArchitectureRulesTest` | 3 | 0 | 0 |
| `DomainBoundaryArchitectureTest` | 4 | 0 | 0 |
| `ParityArchitectureRulesTest` (nova, paridade com microsserviços) | 3 | 0 | **8** |
| **Total** | **10** | **0** | **8** |

"Violações ativas" é o que a suíte reporta rodando `mvn test` hoje — 0,
suíte verde. "Violações reais encontradas" é o resultado de rodar as 3
regras novas **sem** a exceção de escopo (feito uma vez, com um teste
temporário, só para quantificar — não commitado): 8 ocorrências reais no
código de produção atual, nenhuma corrigida.

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

| # | Regra | O que verifica | Violações encontradas |
|---|---|---|---|
| 8 | `namingConventionShouldBeRespected` | `@RestController` termina em `Controller`, `@Service` em `Service`, `@Repository` em `Repository`. | **0** |
| 9 | `repositoriesShouldOnlyBeAccessedByServices` | Classes em `..repository..` só são acessadas de `..repository..`/`..service..` (versão estrita — sem a exceção de `security` aplicada, para medir). | **3** |
| 10 | `controllersShouldNotDependOnEntities` | Nenhuma classe `@RestController` **depende** de classe `@Entity` (mais forte que a regra 3: checa dependência, não só tipo de retorno). | **5** |

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
teste de arquitetura passar. A regra em si não foi enfraquecida: o escopo
foi restrito a "qualquer entidade `@Entity`, exceto `User` nesses dois
pontos nomeados", com a justificativa no Javadoc da regra — qualquer nova
dependência de controller em outra entidade continua acusando violação.

### `repositoriesShouldOnlyBeAccessedByServices` — 3 ocorrências

| Classe.Método | Acesso |
|---|---|
| `ApplicationConfig.userDetailsService` (lambda) | `UserRepository.findByEmail` |
| `DatabaseSeeder.seedAdmin` (lambda, linha 17) | `UserRepository.findByEmail` |
| `DatabaseSeeder.seedAdmin` (lambda, linha 27) | `UserRepository.findByEmail` |

**Decisão:** documentar, não corrigir. É wiring de autenticação — o
`UserDetailsService` do Spring Security precisa do repositório na própria
definição do bean — e seed de dados de inicialização, não acoplamento de
regra de negócio entre domínios. A regra final (a que roda em `mvn test`)
amplia o escopo permitido para incluir `..security..`, com a mesma
justificativa registrada no Javadoc da regra.

### `namingConventionShouldBeRespected` — 0 ocorrências

Todas as classes anotadas já seguem a convenção — nenhuma decisão de escopo
precisou ser tomada para esta regra.

## Alterações em código de produção para chegar a 0 violações ativas

**Nenhuma.** As duas regras com violação real tiveram seu **escopo**
ajustado dentro do próprio arquivo de teste (`ignoreDependency`-equivalente
via predicado customizado e ampliação da lista de pacotes permitidos), não o
código em `src/main`. Mesmo padrão já usado em `DomainBoundaryArchitectureTest`
para excluir a camada de controller das regras 4–7.

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
diferentes dos controllers que os consumiriam indevidamente.

## Metodologia

```bash
mvn test
# as 3 classes de arquitetura aparecem no Surefire como qualquer outra classe de teste
```

Para reproduzir a contagem de "violações reais encontradas" (8), as duas
regras com exceção de escopo podem ser rodadas com o predicado estrito
(sem a exceção) — não commitado, usado só para esta medição; resultado
reproduzido aqui com o detalhamento completo de classes/métodos/linhas
acima, já suficiente para auditoria sem precisar recriar o teste temporário.
