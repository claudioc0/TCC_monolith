# Relatórios de Métricas — Monólito

Snapshot dos relatórios brutos gerados por JaCoCo e PITest, versionado para
servir de artefato de entrega (ver `PLANO DE SPRINTS — ACOMPANHAMENTO`, Sprint 1).
Normalmente esses relatórios ficam só em `target/` (ignorado pelo Git e apagado
a cada `mvn clean`) — esta pasta é uma cópia manual do estado mais recente.

> **Nota de metodologia:** uma medição anterior de branch coverage reportou
> 90,1% usando as colunas erradas do CSV do JaCoCo (instruções de bytecode em
> vez de ramos). Foi corrigido após verificação independente por um segundo
> integrante do grupo — o valor real é bem menor (ver `cobertura-codigo.md`,
> seção "Correção de metodologia"). A mesma verificação encontrou um mutante
> sobrevivente adicional no PITest (`OrderStatus.CONFIRMADO`), já corrigido —
> ver `mutation-score.md`. Violações arquiteturais foram conferidas de forma
> independente também, sem divergência.

## Conteúdo

- **[`cobertura-codigo.md`](./cobertura-codigo.md)** — relatório de Branch
  Coverage em Markdown: resultado geral (corrigido), tabela por pacote,
  leitura dos dados, a correção de metodologia em detalhe.
- **[`mutation-score.md`](./mutation-score.md)** — relatório de Mutation
  Score em Markdown: KILLED/SURVIVED/NO_COVERAGE, mutantes sobreviventes
  documentados individualmente, histórico das rodadas de refinamento.
- **[`violacoes-arquiteturais.md`](./violacoes-arquiteturais.md)** —
  relatório de violações arquiteturais (ArchUnit) em Markdown: as 7 regras,
  as 11 violações encontradas e corrigidas, estado atual, comparação com a
  versão em microsserviços.
- **[`execution-log.md`](./execution-log.md)** — log acumulado de execuções
  das três métricas, para evidenciar determinismo/ausência de flakiness.
- **[`replicacao-e-determinismo.md`](./replicacao-e-determinismo.md)** —
  quantas rodadas de medição são recomendadas (e por quê), e o critério usado
  para classificar cada divergência encontrada entre a medição própria e a
  verificação independente do Eric (erro / achado de ambiente / estados
  diferentes no tempo).
- **`Relatorio-*.docx`** — versões completas em Word das três medições acima,
  produzidas de forma independente por outro integrante do grupo (mesma
  metodologia, ambiente JDK 21 em vez de JDK 17); os `.md` desta pasta
  incorporam as correções e achados desses documentos.
- **`jacoco/index.html`** — relatório bruto de Branch Coverage (HTML,
  navegável por pacote/classe/linha).
- **`pitest/index.html`** — relatório bruto de Mutation Score (HTML, com o
  detalhe de cada mutante gerado e qual teste o matou).

## Como reproduzir

```bash
mvn test                                          # regenera jacoco/
mvn org.pitest:pitest-maven:mutationCoverage      # regenera pitest/
```

Os relatórios saem em `target/site/jacoco/` e `target/pit-reports/`
respectivamente; esta pasta é uma cópia manual do estado mais recente.

## Mapeamento com o plano de sprints

| Artefato do plano | Onde está |
|---|---|
| Relatório de cobertura de código (JaCoCo) | `jacoco/index.html` |
| Relatório de mutation score (PITest) | `pitest/index.html` |
| Suíte de testes de arquitetura (ArchUnit) | `src/test/java/.../architecture/` |
| Testes unitários (JUnit 5) | `src/test/java/.../*ServiceTest.java`, `GlobalExceptionHandlerTest`, `JwtAuthenticationFilterTest`, `ApplicationConfigTest`, `SecurityConfigTest` |
| Testes de integração (JUnit 5 + MockMvc) | `src/test/java/.../integration/*IntegrationTest.java` |
| Documento de métricas do monólito | [Artifact publicado](https://claude.ai/code/artifact/eebc5cef-cbdf-4b91-9075-5eb370e4e46c) |
| Repositório Git organizado | este repositório (`.gitignore`, `target/` destrackeado, raiz correta) |
