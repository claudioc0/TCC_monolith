# Replicação, Determinismo e Reconciliação de Divergências

Este documento registra duas discussões metodológicas que surgiram durante a
coleta de métricas do monólito, mas que não pertenciam a nenhum dos três
relatórios por métrica: quantas rodadas de medição são necessárias, e como
decidir se uma diferença entre duas medições independentes é um erro, um
achado científico legítimo, ou nenhum dos dois.

## 1. Quantas rodadas de teste são recomendadas para maior assertividade?

A resposta depende de qual tipo de variação se está tentando controlar, e é
diferente do que a intuição sugere para as três métricas deste protocolo.

**Ponto central: branch coverage, mutation score e violações arquiteturais
são determinísticas neste projeto — verificado empiricamente, não assumido.**
JaCoCo produziu hash MD5 idêntico do `jacoco.csv` em 3 execuções consecutivas;
PITest produziu o mesmo status para cada um dos 171 mutantes, mutante a
mutante, em todas as rodadas; ArchUnit é análise estática sem nenhuma fonte de
aleatoriedade. Repetir uma medição determinística 10× ou 100× produz o mesmo
número todas as vezes — não há variância real para "mais rodadas" reduzirem.
Isso contrasta com métricas de performance/tempo (fora do escopo deste
protocolo), onde repetição serve para calcular média e desvio-padrão sobre
ruído real (aquecimento de JVM, contenção de CPU).

Dado isso, o número de rodadas na mesma máquina **não é a alavanca certa**
para aumentar assertividade aqui. As que são, em ordem de valor:

1. **3 execuções na mesma máquina** — suficiente para descartar teste
   instável (*flaky*); já feito e documentado em `execution-log.md`. Rodar
   mais do que isso sem motivo específico não muda o resultado quando as 3
   primeiras já saíram idênticas.
2. **Replicação independente, em ambiente diferente** — tem valor real,
   porque pega classes de erro que repetição na mesma máquina não pega: erro
   de metodologia, configuração de ambiente, viés pessoal na leitura dos
   dados. A verificação do Eric (JDK 21 em vez de 17) cumpriu esse papel e
   encontrou um erro real (seção 2 abaixo) que nenhuma quantidade de
   repetição local teria revelado.
3. **Alinhar o protocolo de medição entre monólito e microsserviços** — pesa
   mais na validade da comparação final do que qualquer repetição adicional
   de qualquer um dos lados. Três divergências de protocolo foram
   registradas como ameaça à validade ao longo do projeto: versão do
   PITest, critério de exclusão de classes de bootstrap/seed do escopo de
   mutação, e a versão do JDK. As três já foram resolvidas: PITest alinhado
   em 1.15.3 (resultado idêntico ao de antes — a divergência de ambiente
   nunca foi causada pela versão do PITest), exclusão de classes removida
   (o score real era 1,5 p.p. menor do que o reportado), e o JDK padronizado
   em 21 por pedido do orientador (2026-09-11) — ver `mutation-score.md`
   para o efeito de cada mudança.

**Recomendação prática adotada:** manter as 3 rodadas locais como evidência de
determinismo, contar a replicação do Eric como a segunda camada de rigor, e
— se for reforçar ainda mais o capítulo de metodologia — a próxima ação de
maior valor é configurar CI (uma terceira verificação independente, em
ambiente limpo), não repetir localmente de novo.

## 2. Reconciliando as diferenças entre a medição própria e a do Eric

Duas medições independentes do mesmo sistema produziram números diferentes em
alguns pontos. Nem toda diferença é do mesmo tipo — importa saber qual, antes
de decidir o que corrigir.

### 2.1 Branch coverage: 90,1% (medição própria, original) vs. 78,6% (Eric) — **erro real, não diferença legítima**

Não foi um caso de duas medições válidas discordando. A medição original leu
as colunas `INSTRUCTION_MISSED`/`INSTRUCTION_COVERED` do `jacoco.csv` como se
fossem `BRANCH_MISSED`/`BRANCH_COVERED`. Ao corrigir para a mesma metodologia
usada pelo Eric, o resultado bateu com o dele (e depois divergiu de novo, mas
pelo motivo da seção 2.3, não por erro). A lição: quando dois métodos
independentes dão números muito distantes, o desempate é sempre o dado bruto
— foi comparar contra o CSV que expôs o erro, não intuição sobre qual
resultado "parecia mais certo".

Detalhe completo em `cobertura-codigo.md`, seção "Correção de metodologia".

### 2.2 Mutation score — total de mutantes: 171 (medição própria, JDK 17) vs. 167 (Eric, JDK 21) — **diferença real e esperada, não erro de ninguém**

O PITest muta bytecode compilado, não código-fonte. Compiladores de JDKs
diferentes geram bytecode ligeiramente diferente para a mesma classe Java
(tabelas de switch de enum, invokedynamic de lambda/concatenação de string,
métodos sintéticos de record) — menos instrução às vezes significa menos alvo
de mutação. 4 mutantes de diferença em 171 (≈2,3%) é magnitude pequena e
plausível para essa causa, e é um fenômeno documentado na literatura de
mutation testing em nível de bytecode.

Na época, o critério de desempate era qual ambiente batia com o que o
projeto declarava: `<java.version>17</java.version>` no `pom.xml` tornava
JDK 17 o valor de referência. O achado do Eric não foi descartado — virou
uma nota de reprodutibilidade registrada em `mutation-score.md`: "rode a
mesma suíte" não basta como instrução de replicação; o JDK usado precisa
estar declarado junto do número.

> **Atualização (2026-09-11):** por pedido do orientador, essa divergência
> deixou de ser só documentada e foi **eliminada** — o monólito migrou para
> JDK 21, o mesmo ambiente que o Eric já usava. Não foi mais preciso
> escolher "qual lado é a referência": os dois convergiram para o mesmo. O
> efeito, medido diretamente (não mais inferido): mutation score do
> monólito caiu de 81,0% (141/174, em JDK 17) para 80,6% (137/170, em
> JDK 21) — 4 mutantes a menos, mesmos 6 sobreviventes, nenhuma conclusão
> qualitativa muda. Esta seção fica como registro do raciocínio usado
> *antes* de decidir migrar, não como o estado atual do projeto — ver
> `mutation-score.md` e `resultados-finais.md` para os números vigentes.

### 2.3 Mutation score — mortos: 140 (medição própria, antes) vs. 136 (Eric) → 141 (medição própria, depois) — **não é discrepância de medição, é sequência temporal**

O Eric mediu, encontrou um mutante sobrevivente adicional
(`OrderStatus.CONFIRMADO` aceitando qualquer transição, por faltar teste para
os casos bloqueados a partir desse estado) e reportou. A correção desse gap
(dois testes novos em `OrderStatusTransitionTest`) foi feita depois do
relatório dele já existir. Não são duas medições simultâneas do mesmo estado
de código — são o "antes" e o "depois" da mesma correção. Comparar os dois
como se devessem bater seria um erro de análise, não uma divergência real a
explicar.

### 2.4 Violações arquiteturais: 0/7 nos dois lados — **concordância total, sem ajuste necessário**

Nenhuma divergência aqui. Serve de controle: quando as condições são
idênticas (mesmo código, mesma versão de ferramenta, nenhuma correção entre
as duas medições), os dois métodos independentes convergem exatamente, como
esperado.

## 3. O teste geral para classificar uma divergência

Ao encontrar um número diferente entre duas medições independentes do mesmo
sistema, a pergunta que decide a categoria é sempre a mesma:

> **Dá para explicar a causa raiz de forma mecânica** (coluna errada, JDK
> diferente, código mudou no meio) **?**

- **Sim, e a causa é um erro de leitura/configuração** → é bug; corrigir e
  registrar a correção (não substituir o número silenciosamente).
- **Sim, e a causa é uma variável de ambiente/ferramenta legítima** → é
  achado científico; documentar como nota de reprodutibilidade ou ameaça à
  validade, decidir qual ambiente é a referência, manter os dois registrados.
- **Sim, e a causa é que o código mudou entre as duas medições** → não é uma
  divergência de medição, é comparar dois estados diferentes; não tratar como
  discrepância.
- **Não — mesmo código, mesmo ambiente, mesma versão de ferramenta, e ainda
  assim os números diferem** → esse é o único caso que de fato indica teste
  instável (*flaky*) ou erro em um dos dois lados, e exige investigação antes
  de publicar qualquer um dos dois números.

Todas as divergências encontradas neste projeto até agora caem nas três
primeiras categorias — nenhuma no quarto caso, ou seja, nenhuma evidência de
flakiness real na suíte.
