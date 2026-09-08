# Resultados Finais — Monólito (estado consolidado)

Ponto único de consulta para "qual é o número atual de cada métrica" — os
demais arquivos desta pasta têm a análise detalhada por métrica; este junta
os quatro resultados no mesmo lugar e resolve, de forma explícita, qual valor
é o de referência quando existiu mais de uma medição ao longo do tempo.

**Commit de referência:** pós-push de 2026-09-04 (alinhamento de versão do
PITest + remoção da exclusão de classes, ambos deste mesmo dia)
**Ambiente de referência:** JDK 17.0.18 (`<java.version>17</java.version>`
no `pom.xml`) — ver seção "Ambiente" abaixo para o porquê de fixar isso.

## Tabela consolidada

| Métrica | Ferramenta | Resultado atual | Regra de aceite do protocolo |
|---|---|---|---|
| Testes (unidade + integração + arquitetura) | JUnit 5 + Mockito + ArchUnit | **98/98, 0 falhas, 0 erros** (69 são de unidade, 29 de integração) | suíte verde |
| Branch Coverage | JaCoCo 0.8.11 | **34/42 = 81,0%** (73,8% só com os 69 testes de unidade — ver ameaça 4) | meta do protocolo: 100% (fonte: `TCC_Métricas.docx`, não confirmada em outro documento — ver observação abaixo) |
| Mutation Score | PITest 1.15.3 | **141/174 = 81,0%** (Test Strength 141/147 = 95,9%) | sem meta fixa; comparativo entre arquiteturas |
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

### Mutation Score: 52,6% (rascunho inicial) → 81,9% → 82,5% → 81,0%

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
   (141/174)** — valor de referência atual. A exclusão estava inflando o
   score em 1,5 p.p.; os 3 mutantes que ela escondia (todos em
   `DatabaseSeeder`) sobrevivem de verdade.

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

Detalhe da ameaça 2: `mutation-score.md`, seção "Uma ameaça resolvida, outra
fechada nesta rodada". Detalhe da ameaça 4: `cobertura-codigo.md`, seção
"Comparação com a versão em microsserviços".

## Ambiente

JDK 17 é o valor de referência porque é o que o `pom.xml` declara
(`<java.version>17</java.version>`). A verificação independente do Eric em
JDK 21 obteve totais ligeiramente diferentes em dois pontos — não por erro,
mas porque PITest muta bytecode compilado, e compiladores de JDKs diferentes
geram bytecode ligeiramente distinto para a mesma classe fonte:

| | JDK 17 (referência) | JDK 21 (verificação Eric) |
|---|---|---|
| Mutantes totais gerados | 171 (configuração antiga, com exclusão) | 167 |
| Branch coverage | 81,0% (34/42) | mesmo código-fonte, mesma suíte — sem divergência de ramos reportada |

A comparação de mutantes por JDK ainda não foi refeita sobre os 174
mutantes da configuração atual (sem exclusão) — só foi medida, até agora,
com a configuração antiga, em que os dois lados (JDK 17 e JDK 21) usavam a
mesma exclusão de classes.

Ver `mutation-score.md`, seção "Ambiente: por que o JDK declarado importa",
e `replicacao-e-determinismo.md`, seção 2.2, para a análise completa de por
que essa diferença é esperada e não indica erro de nenhum dos dois lados.

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
