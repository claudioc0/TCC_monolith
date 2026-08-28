# Relatório de Violações Arquiteturais (ArchUnit)

**Ferramenta:** ArchUnit 1.2.1 (`archunit-junit5`)
**Sistema:** monolith-ecommerce
**Commit de referência:** `a5fe3a0`
**Critério medido:** densidade de violações — dependências reais no bytecode
compilado que contradizem regras de arquitetura declaradas explicitamente,
verificadas estaticamente a cada execução de `mvn test`.

## Resultado atual

| Suíte | Regras | Violações |
|---|---|---|
| `CommonArchitectureRulesTest` | 3 | **0** |
| `DomainBoundaryArchitectureTest` | 4 | **0** |
| **Total** | **7** | **0** |

Consistente em todas as execuções registradas (ver `execution-log.md`) — 0
violações não é uma amostra única, é o resultado estável desde a correção
inicial descrita abaixo.

## As 7 regras

### Regras comuns (`CommonArchitectureRulesTest`)

| # | Regra | O que verifica |
|---|---|---|
| 1 | `controllers_should_not_access_repositories_directly` | Nenhuma classe em `..controller..` pode depender de classes em `..repository..` — acesso ao domínio só através da camada de Service. |
| 2 | `services_should_not_depend_on_controllers` | Nenhuma classe em `..service..` pode depender de classes em `..controller..` — a camada de negócio não conhece a camada de apresentação. |
| 3 | `controller_endpoints_should_not_return_entities` | Nenhum método público de um Controller retorna uma entidade `@Entity` diretamente. Verificado via reflection sobre o **tipo genérico real** de retorno (o `T` de `ResponseEntity<T>`), não só o tipo apagado — pega o caso de um DTO ser trocado por uma entidade por engano em qualquer endpoint. |

### Regras de fronteira entre domínios (`DomainBoundaryArchitectureTest`)

| # | Regra | O que verifica |
|---|---|---|
| 4 | `orders_should_not_access_other_domains_internals` | Classes em `orders.service`/`orders.repository` não podem depender de `entity`/`repository` de `products`, `users` ou `reports`. |
| 5 | `products_should_not_access_other_domains_internals` | Mesma restrição, a partir de `products`. |
| 6 | `users_should_not_access_other_domains_internals` | Mesma restrição, a partir de `users`. |
| 7 | `reports_should_not_access_other_domains_internals` | Mesma restrição, a partir de `reports` — inclui `orders`, já que `reports` agrega dados de pedidos e deveria fazer isso via `OrderService`, não acessando `OrderRepository`/`OrderStatus` diretamente. |

**Por que a camada de controller fica fora das regras 4–7:** `@AuthenticationPrincipal User currentUser` é uma integração de framework (Spring Security), não acoplamento de negócio entre domínios — incluir isso na regra geraria ruído sem sinal real de violação. Essa é uma decisão de escopo, documentada aqui para quem lê "0 violações" saber dela: a versão em microsserviços usa uma regra mais forte ("nenhuma classe `@RestController` depende de classe `@Entity`", por dependência, não só tipo de retorno) — aplicada do jeito que está no monólito, essa regra mais forte acusaria violação exatamente em `UserController` e `OrderController`, por causa desse mesmo parâmetro. É uma diferença conhecida entre as duas suítes, não algo escondido.

## Violações encontradas na primeira execução (já corrigidas)

Quando essas regras foram escritas e rodadas pela primeira vez contra código
que já tinha passado por revisão manual, encontraram **11 violações reais**,
distribuídas em 2 pontos:

| # | Violação | Ocorrências | Correção aplicada |
|---|---|---|---|
| 1 | `OrderService.create()` chamava `Product.getId()`/`getName()`/`getPrice()` diretamente sobre a entidade JPA de `products`, em vez de depender só de `ProductService`. | 3 | `ProductService.findById()` passou a expor um DTO (`ProductResponse`); `ProductService.getProductOrThrow()` virou `private`. |
| 2 | `ReportService` injetava `OrderRepository` diretamente para agregar dados de vendas, ignorando `OrderService`. | 8 | `OrderService` ganhou métodos de agregação por período (`findByPeriod`, `countDeliveredByPeriod`, `countCanceledByPeriod`, `sumRevenueByPeriod`); `ReportService` passou a depender só de `OrderService`. |

Nenhuma das 11 foi encontrada por revisão de código — o código já tinha sido
revisado antes de essas regras existirem. Foram encontradas porque a checagem
é automatizada e exaustiva, não amostral: é o argumento empírico, dentro do
próprio projeto, de que verificação de arquitetura executável encontra
acoplamento indevido que revisão manual não escala para pegar.

## Por que essa métrica importa especificamente para a comparação com microsserviços

Num monólito, nada no compilador ou no runtime impede uma classe de importar
qualquer outra classe do mesmo artefato — a fronteira entre `orders` e
`products` existe só por convenção, e só o ArchUnit a torna visível e
mensurável. Numa decomposição em microsserviços, a mesma fronteira passa a
ser imposta fisicamente: sem compartilhamento de classpath entre serviços, o
acoplamento indevido deixa de ser uma violação de estilo e se torna,
literalmente, impossível de compilar. A densidade de violações no monólito é
a medida direta do que a fronteira física do microsserviço substitui.

## Comparação com a versão em microsserviços

| Item | Monólito | Microsserviços |
|---|---|---|
| Versão do ArchUnit | 1.2.1 | 1.2.1 |
| Classes de regras | 2 | não detalhado |
| Regras executadas | 7 | 17 (6 regras distintas, replicadas por serviço) |
| Violações no estado final | 0 | 0 |

A diferença de 7 para 17 não significa que o lado dos microsserviços seja mais
rigoroso — são 6 regras distintas repetidas em cada serviço (cada um é um
projeto Maven separado, com sua própria suíte). Em critério de regra
efetivamente distinta, o monólito verifica 7 e os microsserviços 6; as 3 que
só existem do lado de lá (convenção de nomes, acesso a repositório restrito a
services, e a versão forte da regra de entidades-em-controller citada acima)
estão registradas como diferença conhecida, não como algo a esconder.

## Metodologia

As regras de arquitetura rodam como parte normal de `mvn test` (JUnit 5 via
`@AnalyzeClasses`/`@ArchTest`), sem goal ou configuração adicional:

```bash
mvn test
# CommonArchitectureRulesTest e DomainBoundaryArchitectureTest
# aparecem no relatório do Surefire como qualquer outra classe de teste
```

Código-fonte das regras: [`src/test/java/.../architecture/`](../../src/test/java/com/pucpr/tcc/monolith/architecture/).

Verificado de forma independente por um segundo integrante do grupo (3
rodadas, resultado idêntico), com análise adicional da decisão de escopo
citada acima. Relatório completo em Word:
[`Relatorio-Violacoes-Arquiteturais-ArchUnit.docx`](./Relatorio-Violacoes-Arquiteturais-ArchUnit.docx).
