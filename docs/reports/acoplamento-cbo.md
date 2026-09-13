# Relatório de Acoplamento Estrutural (CBO)

**Ferramenta:** [`ck`](https://github.com/mauricioaniche/ck) 0.7.0 (Aniche et al.) — calcula métricas CK
(Chidamber & Kemerer) por análise estática do código-fonte, sem precisar de bytecode compilado
**Sistema:** monolith-ecommerce
**Ambiente:** JDK 21 — análise estática, sem dependência da versão de runtime
**Critério medido:** CBO (*Coupling Between Objects*) — número de outras classes
às quais uma classe está acoplada (tipo usado em campo, retorno de método,
parâmetro, variável local), conforme Chidamber & Kemerer (1994). É a Question 4
do protocolo GQM deste experimento: quanto uma classe depende de outras, ou
seja, quantos colaboradores um teste de unidade precisa isolar (mockar) para
exercitá-la de forma determinística.

## Resultado geral

29 classes "de negócio" analisadas (exclui as 5 subclasses anônimas por
constante do enum `OrderStatus`, que têm CBO=1 cada e são artefato do padrão
"enum com método abstrato por constante", não unidades de design
independentes).

| Classe | CBO | WMC | RFC | LOC |
|---|---|---|---|---|
| `OrderController` | 16 | 7 | 10 | 24 |
| `ProductController` | 15 | 7 | 11 | 25 |
| `UserService` | 14 | 8 | 23 | 39 |
| `Order` | 13 | 15 | 8 | 55 |
| `GlobalExceptionHandler` | 13 | 12 | 11 | 37 |
| `SecurityConfig` | 12 | 3 | 24 | 19 |
| `OrderService` | 12 | 14 | 30 | 55 |
| `ApplicationConfig` | 12 | 5 | 7 | 21 |
| `UserController` | 11 | 5 | 7 | 19 |
| `JwtAuthenticationFilter` | 11 | 10 | 15 | 43 |
| `AuthController` | 10 | 3 | 5 | 12 |
| `User` | 10 | 15 | 3 | 55 |
| `ProductService` | 9 | 11 | 22 | 47 |
| `ReportController` | 9 | 2 | 2 | 9 |
| `OrderItem` | 8 | 9 | 2 | 38 |
| `JwtService` | 7 | 11 | 26 | 43 |
| `Product` | 7 | 15 | 1 | 57 |
| `OrderRepository` | 6 | 5 | 0 | 7 |
| `DatabaseSeeder` | 6 | 3 | 4 | 15 |
| `ReportService` | 5 | 2 | 7 | 16 |
| `UserRepository` | 3 | 2 | 0 | 4 |
| `ProductRepository` | 3 | 2 | 0 | 4 |
| `MonolithApplication` | 2 | 1 | 1 | 5 |
| `OrderStatus` | 1 | 1 | 0 | 28 |
| `InvalidStatusTransitionException` | 1 | 1 | 1 | 5 |
| `ProductNotFoundException` | 0 | 1 | 0 | 5 |
| `UserRole` | 0 | 0 | 0 | 1 |
| `InsufficientStockException` | 0 | 1 | 1 | 5 |
| `OrderNotFoundException` | 0 | 1 | 0 | 5 |

**Média de CBO: 7,45** (mediana 9; considerando as 5 subclasses anônimas do
`OrderStatus`, cai para 6,5 sobre 34 unidades — número menos representativo,
por isso a média de referência é sobre as 29 classes de negócio).

## CBO médio por camada

| Camada | Classes | CBO médio |
|---|---|---|
| `controller` | 5 | 12,20 |
| `service` | 4 | 10,00 |
| `security` | 5 | 9,60 |
| `entity` | 6 | 6,50 |
| `repository` | 3 | 4,00 |
| `exception` | 5 | 2,80 |
| raiz (`MonolithApplication`) | 1 | 2,00 |

## Leitura dos dados

O padrão é o oposto do que se vê em branch coverage e mutation score: lá,
`controller` é a camada **mais fraca** em cobertura/detecção; aqui, é a
camada com **maior acoplamento**. Faz sentido — um controller depende de
DTO de request, DTO de response, Service, exceptions tratadas e tipos do
Spring MVC (`ResponseEntity`, anotações de mapeamento) simultaneamente, e é
exatamente por isso que testar um controller isolado exige mockar mais
colaboradores.

`security` aparece com CBO médio alto (9,6) pelo mesmo motivo estrutural:
`SecurityConfig`/`ApplicationConfig` fazem a integração entre JWT, Spring
Security, `PasswordEncoder`, `AuthenticationManager` e os filtros — é código
de *wiring*, categoria que também concentra os mutantes residuais aceitos em
`mutation-score.md`. Os dois pontos são reflexos da mesma causa raiz: código
de configuração/integração de framework é estruturalmente mais acoplado, e
paga esse preço tanto em CBO quanto em testabilidade por mutação.

`exception` e `repository` têm o CBO mais baixo — exceptions são folhas
(dependem só de `RuntimeException`/campos simples) e repositórios são
interfaces Spring Data (uma dependência: a entidade que gerenciam), o que
também explica o 100% de mutation score e branch coverage dessas camadas:
pouco acoplamento deixa pouca superfície para esconder defeito.

## Por que este número importa para a comparação com microsserviços

Chidamber & Kemerer estabelecem CBO como preditor de manutenibilidade e
testabilidade — quanto mais uma classe depende de outras, mais cara é para
isolar em teste e mais frágil a mudanças alheias. No monólito, uma
dependência funcional entre domínios (ex.: "pedido precisa consultar
estoque de produto") é acoplamento de objeto Java direto — `OrderService`
injeta `ProductService` na mesma JVM, e esse acoplamento entra no CBO da
classe. Numa decomposição em microsserviços, essa mesma dependência deixa de
ser um acoplamento de objeto: vira uma chamada HTTP contra um DTO local, que
nunca é a mesma classe do outro serviço. O CBO Java "entre domínios" tende a
zerar por construção — o que a arquitetura de microsserviços troca é
acoplamento de objeto por acoplamento de contrato de rede, que esta métrica,
por definição, não captura (é o motivo de a métrica 4 do protocolo GQM ser
"CBO", não "acoplamento total" — o recorte é deliberado).

## Limitação de metodologia (declarada)

A análise rodou com `useJars=false` — o `ck` resolve tipos a partir só do
código-fonte do projeto, sem o classpath completo de bibliotecas externas
(Spring, JPA, JJWT). Isso significa que tipos de terceiros usados em campos/
parâmetros/retornos **contam** para o CBO (a ferramenta reconhece o tipo
pelo nome, mesmo sem resolver a biblioteca), mas a resolução de hierarquia
completa desses tipos (para métricas como DIT/NOC) pode ser menos precisa.
Para CBO especificamente — o que importa aqui — o efeito é mínimo, mas fica
registrado como decisão de metodologia, não escondida.

## Comparação com microsserviços

O lado de microsserviços rodou `ck` 0.7.0 (mesma versão) sobre os 4 serviços
em 2026-09-13, respondendo a Question 4 do protocolo GQM:

| Serviço | Classes | CBO médio | CBO máximo |
|---|---:|---:|---:|
| user-service | 13 | 6,77 | 14 (`UserService`) |
| product-service | 10 | 7,10 | 17 (`ProductController`) |
| order-service | 16 | 6,69 | 19 (`OrderController`) |
| report-service | 9 | 6,22 | 11 (`SecurityConfig`) |
| **Agregado (microsserviços)** | **48** | **6,71** | — |
| **Monólito** | **29** | **7,45** | 16 (`OrderController`) |

**Leitura honesta do número, não só o número em si:**

1. **Parte da queda é troca de tipo de acoplamento, não redução real.**
   Uma dependência funcional entre domínios que no monólito é injeção de
   objeto (`OrderService` → `ProductService`, CBO direto) virou, em
   microsserviços, uma classe cliente HTTP (`ProductClient`, CBO 9;
   `OrderClient`, CBO 7) — acoplamento de contrato de rede, que o CBO não
   mede. Reportar só "6,71 < 7,45, logo microsserviços é mais desacoplado"
   contaria uma história incompleta.
2. **Os denominadores não são diretamente comparáveis.** 29 classes contra
   48 — a decomposição em 4 serviços naturalmente duplica infraestrutura
   (cada serviço tem seu próprio `SecurityConfig`, suas próprias exceptions,
   etc.), o que dilui a média para baixo mecanicamente, independente de
   qualquer redução real de acoplamento de negócio. Comparar CBO médio por
   **camada** (`controller` com `controller`, `service` com `service`)
   controla parcialmente esse efeito melhor do que comparar só o agregado.
3. **O padrão por camada se repete dos dois lados:** `controller` continua
   sendo a camada mais acoplada em ambos (`OrderController` é o CBO máximo
   tanto no monólito, 16, quanto — de forma ainda mais acentuada — em
   microsserviços, 19) — consistente com a leitura da seção anterior de que
   é a camada que mais orquestra colaboradores, independente da arquitetura.

Detalhe completo do lado de microsserviços:
[`FECHAMENTO_PENDENCIAS-microservicos.md`](./FECHAMENTO_PENDENCIAS-microservicos.md),
item 2, e o `acoplamento-cbo.md` equivalente no repositório deles.

## Metodologia

```bash
# ck não está no projeto como plugin Maven — roda como ferramenta externa,
# contra o código-fonte, sem precisar compilar:
git clone https://github.com/mauricioaniche/ck   # ou baixar via Maven Central
# com as dependências do ck no classpath:
java -cp "<ck-0.7.0.jar>;<dependencias>" com.github.mauricioaniche.ck.Runner \
    src/main/java false 0 false <diretorio-de-saida>/
# gera <diretorio-de-saida>/class.csv e method.csv
```

Dados brutos (nível de classe e de método) versionados em
[`ck/class.csv`](./ck/class.csv) e [`ck/method.csv`](./ck/method.csv).
