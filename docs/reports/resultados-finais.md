# Resultados Finais — Monólito (estado consolidado)

Ponto único de consulta para "qual é o número atual de cada métrica" — os
demais arquivos desta pasta têm a análise detalhada por métrica; este junta
os quatro resultados no mesmo lugar e resolve, de forma explícita, qual valor
é o de referência quando existiu mais de uma medição ao longo do tempo.

**Commit de referência:** pós-migração para JDK 21 (2026-09-11, pedido do
orientador, para padronizar com o ambiente já usado pelo Eric)
**Ambiente de referência:** JDK 21.0.12.1 Temurin (`<java.version>21</java.version>`
no `pom.xml`) — ver seção "Ambiente" abaixo.

## Tabela consolidada

| Métrica | Ferramenta | Resultado atual | Regra de aceite do protocolo |
|---|---|---|---|
| Testes (unidade + integração + arquitetura) | JUnit 5 + Mockito + ArchUnit | **98/98, 0 falhas, 0 erros** (69 são de unidade, 29 de integração) | suíte verde |
| Branch Coverage | JaCoCo 0.8.11 | **34/42 = 81,0%** (73,8% só com os 69 testes de unidade — ver ameaça 4) | meta do protocolo: 100% (fonte: `TCC_Métricas.docx`, não confirmada em outro documento — ver observação abaixo) |
| Mutation Score | PITest 1.15.3 | **137/170 = 80,6%** (Test Strength 137/143 = 95,8%) | sem meta fixa; comparativo entre arquiteturas |
| Violações Arquiteturais | ArchUnit 1.2.1 | **0/7 regras** | meta do protocolo: 0 |

> A meta de "100% de branch coverage" citada na tabela veio de uma frase do
> `TCC_Métricas.docx` ("o protocolo do experimento define como meta 100% dos
> ramos lógicos") — é a única fonte encontrada para essa meta em todo o
> repositório; não foi confirmada em nenhum outro documento do protocolo.
> Vale checar se ainda é a meta oficial antes de usar em qualquer texto final.

Todas as quatro linhas são determinísticas neste projeto — verificado
empiricamente (hash MD5 do `jacoco.csv`, comparação mutante a mutante do
PITest, ausência de fonte de aleatoriedade no ArchUnit), não assumido. Ver
`replicacao-e-determinismo.md`, seção 1.

## De onde vem cada número (linhagem)

Cada métrica passou por correção ou refinamento em algum ponto da coleta.
Este é o histórico resumido — o detalhe completo de cada mudança está no
relatório específico e em `execution-log.md`.

### Branch Coverage: 90,1% → 78,6% → 81,0%

1. Medição própria original: **90,1% (1.785/1.982)** — **valor errado**, leu
   a coluna de instruções de bytecode do CSV do JaCoCo como se fosse a de
   ramos.
2. Verificação independente do Eric encontrou o erro: **78,6% (33/42)**,
   usando as colunas certas (`BRANCH_MISSED`/`BRANCH_COVERED`).
3. Depois de fechar o gap de teste do `OrderStatus` (ver Mutation Score
   abaixo): **81,0% (34/42)** — valor de referência atual.

Detalhe completo: `cobertura-codigo.md`.

### Mutation Score: 52,6% (rascunho inicial) → 81,9% → 82,5% → 81,0% → 80,6%

1. Rascunho bem inicial (commit `e72507d`, 63 testes, 2026-08-07, documentado
   em `TCC_Métricas.docx`): **52,6% (90/171)** — primeira rodada completa,
   antes de qualquer refinamento de asserção; consistente como ponto de
   partida histórico, não como valor atual.
2. Após rodadas de refinamento de asserções (commit `67d5a7b` em diante):
   **81,9% (140/171)**, estável em 3 repetições.
3. Verificação independente do Eric (JDK 21) encontrou um mutante
   sobrevivente adicional não pego antes (`OrderStatus.CONFIRMADO` aceitando
   qualquer transição) — corrigido com dois testes novos.
4. Depois da correção: **82,5% (141/171)**, confirmado idêntico após alinhar
   a versão do PITest com a de microsserviços (1.15.8 → 1.15.3, sem nenhuma
   mudança no resultado).
5. Depois de remover a exclusão de `MonolithApplication`/`DatabaseSeeder`/
   `architecture.*` do escopo de mutação (ver ameaça 2 abaixo): **81,0%
   (141/174)**, em JDK 17. A exclusão estava inflando o score em 1,5 p.p.;
   os 3 mutantes que ela escondia (todos em `DatabaseSeeder`) sobrevivem de
   verdade.
6. Depois da migração para JDK 21 (2026-09-11, pedido do orientador, ver
   ameaça 5 abaixo): **80,6% (137/170)** — valor de referência atual. 4
   mutantes a menos que em JDK 17 (bytecode diferente em `orders.entity` e
   `security`), mesmos 6 sobreviventes de antes — nenhuma conclusão
   qualitativa muda, só o denominador.

Detalhe completo: `mutation-score.md`.

### Violações Arquiteturais: 11 encontradas → 0 (estável desde então)

Regras escritas contra código já revisado manualmente encontraram 11
violações reais na primeira execução (vazamento de entidade entre domínios,
acesso direto a repositório de outro domínio) — corrigidas, nunca mais
reapareceram em nenhuma rodada própria ou do Eric.

Detalhe completo: `violacoes-arquiteturais.md`.

### Testes: 63 → 96 → 98

63 testes no rascunho inicial (`TCC_Métricas.docx`, commit `e72507d`) → 96
depois de expandir a suíte → 98 depois de fechar o gap do `OrderStatus`.

## `TCC_Métricas.docx` — como este documento antigo se encaixa

Esse arquivo (recém-adicionado à pasta, ainda não commitado) é um rascunho
anterior a todas as correções acima: commit `e72507d`, 63 testes, datado de
2026-08-07. Ele reporta branch coverage como "82,0% (1.626/1.982)" — mesmo
padrão do erro descrito acima (1.982 é o total de **instruções**, não de
ramos; o sistema tem 42 ramos reais), o que sugere que o erro de coluna já
existia desde essa versão inicial, e não foi introduzido depois. Não precisa
de correção — é um snapshot histórico, não um relatório vivo — mas não deve
ser citado como número atual em nenhum lugar do TCC.

A "Parte II" desse documento (Fundamentação Científica do Protocolo,
comparação com os 4 microsserviços) tem conteúdo que **não é redundante**
com o resto desta pasta: a formalização Goal/Question/4-Questions do GQM, a
métrica de CBO/acoplamento estrutural (ainda não instrumentada nem no
monólito nem em microsserviços — item de backlog dos dois lados) e os
números de branch coverage por serviço do lado de microsserviços (dados de
2026-08-07, também não conferidos por esta suíte). Vale revisar se ainda
está atualizado antes de usar como fonte para o capítulo de metodologia do
TCC.

## Ameaças à validade — estado atual

| # | Ameaça | Status |
|---|---|---|
| 1 | Versão do PITest divergente entre monólito e microsserviços (1.15.8 vs. 1.15.3) | **Resolvida** — monólito alinhado a 1.15.3, resultado idêntico |
| 2 | Critério de exclusão de classes no PITest (`MonolithApplication`, `DatabaseSeeder`, `architecture.*` excluídos do monólito; sem exclusão documentada em microsserviços) | **Resolvida (2026-09-04)** — exclusão removida; confirmado que ela inflava o score em 1,5 p.p. (82,5% → 81,0%), só por causa de `DatabaseSeeder` |
| 3 | Métrica de CBO/acoplamento estrutural (Question 4 do GQM, `TCC_Métricas.docx` Parte II) | **Em andamento** — ferramenta escolhida (`mauricioaniche/ck`), instrumentação ainda não feita |
| 4 | Assimetria de esforço de teste entre monólito (4 testes de integração `@SpringBootTest`) e microsserviços (segundo o relatório deles, nenhum) | **Quantificada (2026-09-04), não eliminável unilateralmente** — os 4 testes de integração respondem por 7,1 p.p. de branch coverage (81,0% completa vs. 73,8% só unidade); documentado para reportar as duas leituras lado a lado em vez de esconder a assimetria |
| 5 | Divergência de JDK entre monólito (declarava 17) e a verificação do Eric (usava 21) | **Resolvida (2026-09-11)** — projeto migrado para JDK 21 por pedido do orientador; mutation score recalculado (137/170 = 80,6%), branch coverage e violações inalterados. **Pendência fora deste repositório:** o `pom.xml` de microsserviços também precisa declarar 21 formalmente. |

Detalhe da ameaça 2: `mutation-score.md`, seção "Uma ameaça resolvida, outra
fechada nesta rodada". Detalhe da ameaça 4: `cobertura-codigo.md`, seção
"Comparação com a versão em microsserviços". Detalhe da ameaça 5:
`mutation-score.md`, seção "Ambiente: por que a padronização em JDK 21
aconteceu".

## Ambiente

Até 2026-09-11, o monólito declarava JDK 17 enquanto a verificação
independente do Eric usava JDK 21 — uma divergência de ambiente documentada
como ameaça à validade (ameaça 5 acima). Por pedido do orientador, o
projeto passou a padronizar em **JDK 21** também, eliminando essa variável
em vez de só documentá-la:

| | JDK 17 (histórico) | JDK 21 (referência atual) |
|---|---|---|
| Mutantes totais gerados (PITest) | 174 | 170 |
| Mortos | 141 | 137 |
| Mutation Score | 81,0% | **80,6%** |
| Branch Coverage | 81,0% (34/42) | 81,0% (34/42) — idêntico |
| Violações Arquiteturais | 0/7 | 0/7 — idêntico |

Só o Mutation Score foi sensível à troca de JDK (PITest muta bytecode
compilado, e compiladores de JDKs diferentes geram bytecode ligeiramente
distinto para a mesma classe fonte — 4 mutantes a menos em `orders.entity`
e `security`, mesmos 6 sobreviventes de antes). Branch coverage e
violações arquiteturais não usam bytecode gerado por instrumentação
sensível a essa diferença da mesma forma, e saíram idênticos.

Ver `mutation-score.md`, seção "Ambiente: por que a padronização em JDK 21
aconteceu", e `replicacao-e-determinismo.md`, seção 2.2, para a análise
completa de por que a diferença antiga era esperada e como ela foi
resolvida (não só documentada).

## Rastreabilidade

| Este documento | Onde está o detalhe |
|---|---|
| Números por pacote, ramos ainda sem cobertura | `cobertura-codigo.md` |
| Mutantes SURVIVED individuais, mutation score por pacote | `mutation-score.md` |
| As 7 regras de arquitetura, as 11 violações históricas | `violacoes-arquiteturais.md` |
| Log rodada a rodada (datas, commits, resultados) | `execution-log.md` |
| Metodologia de quantas rodadas e como classificar divergências | `replicacao-e-determinismo.md` |
| Versão publicada, navegável, com narrativa e gráficos | [Artifact publicado](https://claude.ai/code/artifact/eebc5cef-cbdf-4b91-9075-5eb370e4e46c) |

## Próxima atualização deste arquivo

Atualizar quando: (a) a métrica de CBO for instrumentada (`ck`), (b) o lado
de microsserviços rodar PITest e o comparativo final entrar em pauta, ou
(c) qualquer um dos quatro números da tabela consolidada mudar.
