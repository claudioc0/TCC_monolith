# Pendências para Fechar as Ameaças à Validade

> **Status: as três pendências abaixo foram fechadas pelo lado de
> microsserviços em 2026-09-13.** Resposta completa deles em
> [`docs/reports/FECHAMENTO_PENDENCIAS-microservicos.md`](./reports/FECHAMENTO_PENDENCIAS-microservicos.md).
> Este documento fica como registro do que foi pedido originalmente —
> cada seção abaixo tem uma nota de fechamento indicando o que foi feito.

Do lado do monólito não sobrou nenhuma ameaça à validade sob controle deste
repositório — as três que restavam dependiam de uma ação no repositório de
microsserviços. Este documento existe para que essa ação fosse concreta e
verificável, não um "alinhar depois" vago.

## 1. Declarar JDK 21 formalmente no `pom.xml`

> ✅ **Fechado (2026-09-13).** JDK 21.0.12.1 declarado nos 4 `pom.xml`,
> confirmado com `mvn -version` e revalidado com `mvn clean test` (171
> testes, 0 falhas). Ver `FECHAMENTO_PENDENCIAS-microservicos.md`, item 1.

**O que é:** o Eric já *roda* a suíte de microsserviços em JDK 21, mas não
está confirmado que o `pom.xml` de lá *declara* essa versão
(`<java.version>` ou `<maven.compiler.release>`). O monólito migrou para
JDK 21 em 2026-09-11, por pedido do orientador, justamente para eliminar
essa variável de ambiente entre os dois lados.

**Por que importa:** sem a declaração formal, "rodei em JDK 21" depende de
qual JDK estava por acaso ativo na máquina de quem executou — não é
garantido nem reproduzível por outra pessoa que clonar o repositório.

**Como fechar:**
1. Abrir o `pom.xml` de cada um dos 4 serviços.
2. Confirmar/ajustar a propriedade de versão do Java para `21`:
   ```xml
   <properties>
       <java.version>21</java.version>
   </properties>
   ```
3. Rodar `mvn -version` na raiz de cada serviço para confirmar qual JDK o
   Maven está de fato usando, e `mvn clean test` para revalidar a suíte
   depois da mudança (importante: `clean` é obrigatório — mudar só a
   versão sem recompilar do zero não tem efeito no bytecode gerado, isso já
   aconteceu aqui e o resultado ficou "grudado" na versão antiga até
   recompilar).
4. Registrar o JDK usado (ex.: `21.0.12.1 Temurin`) no relatório de
   métricas de microsserviços — não basta "JDK 21" genérico, a *patch
   version* do compilador pode mudar o total de mutantes gerados no PITest.

**Quem age:** time de microsserviços (Eric ou quem mantém o `pom.xml` lá).

**Onde registrar quando fechar:** atualizar `mutation-score.md` e
`resultados-finais.md` deste repositório, seção "Ameaças à validade",
marcando o item como resolvido dos dois lados.

## 2. Rodar a métrica de CBO (`ck`) em microsserviços

> ✅ **Fechado (2026-09-13).** `ck` 0.7.0 (mesma versão daqui) rodado nos 4
> serviços — CBO médio agregado **6,71** (48 classes), contra **7,45** (29
> classes) do monólito. Comparação completa, com a leitura sobre
> `ProductClient`/`OrderClient` substituindo acoplamento Java por
> acoplamento de contrato de rede, em `acoplamento-cbo.md` (seção
> "Comparação com microsserviços") e em
> `FECHAMENTO_PENDENCIAS-microservicos.md`, item 2.

**O que é:** a Question 4 do protocolo GQM ("o esforço de isolamento/mock
aumenta ou diminui com a decomposição?") foi instrumentada só do lado do
monólito (CBO médio 7,45 sobre 29 classes — ver `docs/reports/acoplamento-cbo.md`).
Sem o mesmo dado do lado de microsserviços, não existe comparação — só um
número solto.

**Por que importa:** é a única das quatro métricas do protocolo ainda sem
par comparativo. As outras três (branch coverage, mutation score,
violações arquiteturais) já têm números dos dois lados.

**Como fechar:**
1. Baixar a ferramenta `ck` (mesma versão usada aqui, **0.7.0**, para os
   números serem comparáveis metodologicamente):
   ```bash
   # via Maven, num projeto auxiliar com essa única dependência:
   # com.github.mauricioaniche:ck:0.7.0
   mvn dependency:copy-dependencies -DoutputDirectory=libs
   ```
2. Rodar contra o código-fonte de cada serviço (não precisa compilar antes
   — a ferramenta analisa o `.java` diretamente):
   ```bash
   java -cp "libs/*" com.github.mauricioaniche.ck.Runner \
       src/main/java false 0 false <diretorio-de-saida>/
   ```
3. Isso gera `class.csv` (e `method.csv`) com CBO por classe em cada
   serviço. Calcular a média por serviço e, se fizer sentido, por camada
   (`controller`/`service`/`entity`/etc.), do mesmo jeito que
   `acoplamento-cbo.md` fez aqui — para comparar camada com camada, não só
   o agregado.
4. Um ponto conceitual a documentar junto do número: espera-se que classes
   que fariam chamada a outro domínio no monólito (ex.: `OrderService` →
   `ProductService`) tenham, em microsserviços, esse acoplamento trocado
   por um cliente HTTP (`ProductClient` ou similar) — o CBO Java "entre
   domínios" tende a cair, mas isso não é redução de acoplamento real, é
   troca por acoplamento de contrato de rede que o CBO não mede. Vale
   registrar essa leitura ao lado do número, não só o número.

**Quem age:** quem tiver o repositório de microsserviços em mãos (Eric ou
outro integrante).

**Onde registrar quando fechar:** criar um `acoplamento-cbo.md` equivalente
no repositório de microsserviços, e atualizar a ameaça 3 em
`resultados-finais.md` aqui, incluindo a tabela comparativa final.

## 3. Assimetria de esforço de teste (testes de integração)

> ✅ **Fechado (2026-09-13) — Opção A escolhida.** 4 testes de integração
> escritos do lado de microsserviços (um por serviço), usando
> `MockRestServiceServer` só na fronteira de rede entre serviços. Efeito
> medido: branch coverage subiu em 2 dos 4 serviços (user-service 87%→100%,
> order-service 92%→96,4%); os outros 2 já estavam em 100%. Detalhe em
> `FECHAMENTO_PENDENCIAS-microservicos.md`, item 3.

**O que é:** o monólito tem 4 testes de integração `@SpringBootTest`
(`AuthIntegrationTest`, `OrderIntegrationTest`, `ProductIntegrationTest`,
`ReportIntegrationTest`); a versão em microsserviços, segundo o relatório
dela, não tem nenhum. Medido aqui: esses 4 testes respondem por **7,1
pontos percentuais** de branch coverage (81,0% com eles, 73,8% sem — ver
`cobertura-codigo.md`).

**Por que importa:** é uma diferença real de esforço de teste entre os dois
lados, não só de arquitetura — comparar os números de cobertura/mutação
sem mencionar isso atribuiria à arquitetura uma diferença que na verdade
vem de quantidade de teste escrito.

**Como fechar — não é uma tarefa técnica única, é uma decisão de equipe
entre duas opções:**

- **Opção A — equalizar esforço:** escrever testes de integração
  equivalentes do lado de microsserviços (um por serviço, cobrindo o fluxo
  principal, análogo aos 4 existentes aqui). Fecha a assimetria de verdade,
  mas é esforço de desenvolvimento, não só de medição.
- **Opção B — aceitar e reportar as duas leituras lado a lado:** manter a
  assimetria, mas sempre reportar branch coverage "com integração" e "só
  unidade" dos dois lados (como já é feito aqui), deixando explícito qual
  parte da diferença entre monólito e microsserviços vem da arquitetura e
  qual vem de esforço de teste distinto. Não fecha a ameaça, mas evita que
  ela distorça a conclusão sem ninguém perceber.

**Quem decide:** o grupo, junto com o orientador se for uma mudança de
escopo relevante para o tempo restante do projeto — não é algo para
resolver unilateralmente de um lado só.

**Onde registrar quando decidir:** atualizar a ameaça 4 em
`resultados-finais.md`, e se for a Opção A, o número de branch coverage de
microsserviços muda e precisa ser remedido.

## Resumo rápido

| # | Pendência | Status | Resultado |
|---|---|---|---|
| 1 | JDK 21 não declarado formalmente em microsserviços | ✅ Fechado | JDK 21.0.12.1 nos 4 `pom.xml`, revalidado |
| 2 | CBO sem par comparativo | ✅ Fechado | 6,71 (microsserviços, 48 classes) vs. 7,45 (monólito, 29 classes) |
| 3 | Assimetria de testes de integração | ✅ Fechado (Opção A) | 4 testes de integração novos; coverage subiu em 2 dos 4 serviços |

Nenhuma ameaça à validade permanece em aberto entre os dois lados do
experimento — ver `resultados-finais.md` para a tabela consolidada final.
