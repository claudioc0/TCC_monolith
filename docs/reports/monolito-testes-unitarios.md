# Relatório de Equalização da Suíte de Testes (monólito)

**Ferramentas:** JUnit 5, Mockito, AssertJ, Spring Boot Test (`@WebMvcTest`), ArchUnit 1.2.1
**Sistema:** monolith-ecommerce
**Branch:** `feature/equalizacao-testes-monolito` (a partir do commit `9a389db`)
**Critério medido:** composição da suíte por técnica de teste — equalizar o
*tipo* de teste usado em cada camada entre monólito e microsserviços, não
perseguir um número-alvo de testes (ver `prompt-testes-monolito.md`, §5).

## Antes / Depois / Acréscimo por pacote de domínio

| Pacote | Antes | Depois | Acréscimo |
|---|---|---|---|
| `users` | 19 | 56 | +37 |
| `products` | 7 | 36 | +29 |
| `orders` | 19 | 39 | +20 |
| `reports` | 3 | 7 | +4 |
| `security` | 19 | 26 | +7 |
| `exception` | 9 | 9 | 0 |
| `architecture` | 7 | 10 | +3 |
| **Total** | **98** | **180** | **+82** |

(Contagem lida do relatório do Surefire, não de anotações — `Tests run` por
classe, somado por pacote de domínio.)

## Composição por camada

| Camada | Técnica | Antes | Depois |
|---|---|---|---|
| Entidade/domínio | JUnit puro (sem mock) | 13 | 33 |
| Service | Mockito puro (`@InjectMocks`) | 21 | 33 |
| Controller (slice web) | `@WebMvcTest` + `@Import(SecurityConfig, JwtAuthenticationFilter)` | **0** | **40** |
| Security (filtro + JWT) | Mockito puro / `MockHttpServletRequest` | 19 | 26 |
| Exception handler | Chamada direta aos métodos do handler | 9 | 9 |
| Arquitetura | ArchUnit (`@AnalyzeClasses`/`@ArchTest`) | 7 | 10 |
| Integração (`@SpringBootTest`) | Contexto Spring completo | 29 | 29 |
| **Total** | | **98** | **180** |

A linha de Integração não mudou — por regra do protocolo (seção 1 do
prompt), os 4 testes `@SpringBootTest` existentes (`AuthIntegrationTest`,
`OrderIntegrationTest`, `ProductIntegrationTest`, `ReportIntegrationTest`, 29
testes) ficam como estão; todo ganho novo veio de teste unitário ou de
slice, nunca de novo `@SpringBootTest`. O salto de 0 → 40 em Controller é
deliberado: era a maior lacuna identificada (os microsserviços têm 34 testes
`@WebMvcTest` que o monólito não tinha nenhum equivalente).

## O que foi acrescentado, por etapa

1. **ArchUnit — 3 regras de paridade** (`ParityArchitectureRulesTest`):
   `namingConventionShouldBeRespected`, `repositoriesShouldOnlyBeAccessedByServices`,
   `controllersShouldNotDependOnEntities`. Ver `monolito-violacoes-arquiteturais.md`.
2. **Entidade/domínio**: `UserTest`, `ProductTest`, `OrderTest`, `OrderItemTest`
   (só `OrderStatus` tinha teste próprio antes).
3. **Reforço de Service**: `UserService` (findById/findAll/delete/login —
   caminho de autenticação sem usuário encontrado) e `ProductService`
   (searchByName/update/delete — eram os mais fracos).
4. **Slices de Controller**: `ProductControllerTest`, `OrderControllerTest`,
   `UserControllerTest`, `AuthControllerTest`, `ReportControllerTest` — 40
   testes, cadeia de segurança real.
5. **Security**: `DatabaseSeederTest` (não existia nenhum teste desta classe)
   e 3 testes novos em `JwtAuthenticationFilterTest` (header sem prefixo
   `Bearer`, autenticação pré-existente não sobrescrita, `extractUsername`
   retornando `null` sem lançar exceção).

## Decisões de teste que valem registro

### Tokens JWT reais nos testes de slice, não mockados

Em vez de mockar `JwtService` e simular um `SecurityContext` já autenticado
(abordagem mais comum e mais simples), os testes de Controller importam o
`JwtService` real para o contexto do slice e geram um token assinado de
verdade. Só o `UserDetailsService` é mockado — é ele que devolveria o
usuário vindo do banco em produção. Isso faz o `JwtAuthenticationFilter`
rodar exatamente como em produção dentro do slice, em vez de pular a parte
que mais importa testar (a cadeia de segurança real, exigência explícita do
protocolo).

```java
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class ProductControllerTest {
    @Autowired private JwtService jwtService;      // real
    @MockBean private UserDetailsService userDetailsService; // só esse é mock
    @MockBean private AuthenticationProvider authenticationProvider; // wiring do bean, não exercitado pelo fluxo JWT
    ...
}
```

### Helper reutilizado: `SecurityTestSupport`

```java
public static String bearerTokenFor(JwtService jwtService, UserDetailsService userDetailsService, User user) {
    when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
    return "Bearer " + jwtService.generateToken(user);
}
```

Usado pelas 5 classes de slice — registra o usuário no mock e devolve o
header pronto, nunca duplicado entre classes. O id do usuário é definido por
reflexão (`ReflectionTestUtils.setField`) porque `User` não expõe `setId` —
o id só existe depois de persistido, e o teste precisa de um valor
determinístico para comparar contra o que o Controller usa.

### O teste que prova que o `userId` vem do token, não do corpo

```java
@Test
@DisplayName("POST /api/orders deve usar o id do usuário autenticado, não um valor do corpo")
void createShouldUseAuthenticatedUserIdNotBodyValue() throws Exception {
    long authenticatedUserId = 42L;
    when(orderService.create(eq(authenticatedUserId), any(OrderRequest.class)))
            .thenReturn(sampleResponse(authenticatedUserId));
    ...
    verify(orderService).create(eq(authenticatedUserId), any(OrderRequest.class));
}
```

`OrderRequest` não tem campo `userId` — não é um esquecimento, é a defesa
contra impersonação. Este teste é o que fixa esse contrato: usa
`eq(authenticatedUserId)`, então falharia se o Controller algum dia
aceitasse um `userId` vindo de outro lugar.

## O que ficou deliberadamente de fora (e por quê)

| Item | Motivo |
|---|---|
| `SecurityConfig#securityFilterChain`, `ApplicationConfig#authenticationManager` | Wiring de framework (`HttpSecurity`/`AuthenticationConfiguration` internos do Spring); o efeito já é observado indiretamente pelos 40 testes de slice (403/401 corretos, rotas públicas corretas). Cobri-los diretamente exigiria contexto Spring completo — por isso ficam como mutante `SURVIVED` documentado (ver `monolito-mutation-score.md`). |
| `JwtService#isTokenExpired` | Inalcançável no fluxo normal: o parser do JJWT lança `ExpiredJwtException` antes de essa comparação ser avaliada (confirmado pelo teste `isTokenValidShouldReturnFalseForExpiredToken`, que passa pelo caminho da exceção, não pelo `return` mutado). |
| Novos `@SpringBootTest` | Proibido pelo protocolo (seção 1) — toda a cobertura nova veio de unidade/slice, preservando a assimetria medida (monólito com 4 testes de integração completos vs. 1 por serviço nos microsserviços) como dado do experimento, não como algo a esconder. |

## Alterações em código de produção

**Nenhuma.** Toda a equalização desta rodada veio de teste novo ou
reforçado. Nenhuma classe em `src/main` foi tocada.

## Metodologia

```bash
mvn test
# contagem por classe: target/surefire-reports/*.txt ("Tests run: N")
```

Ver `execution-log.md` para as 3 execuções completas desta rodada (180/180
idêntico nas três) e `resultados-finais.md` para o estado consolidado das
quatro métricas.
