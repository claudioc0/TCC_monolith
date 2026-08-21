# Relatórios de Métricas — Monólito

Snapshot dos relatórios brutos gerados por JaCoCo e PITest, versionado para
servir de artefato de entrega (ver `PLANO DE SPRINTS — ACOMPANHAMENTO`, Sprint 1).
Normalmente esses relatórios ficam só em `target/` (ignorado pelo Git e apagado
a cada `mvn clean`) — esta cópia é o snapshot referente ao commit `67d5a7b`.

## Conteúdo

- **`jacoco/index.html`** — relatório de Branch Coverage. 90,1% geral
  (1.785/1.982 ramos), com detalhamento por pacote/classe/linha.
- **`pitest/index.html`** — relatório de Mutation Score. 81,9% geral
  (140/171 mutantes mortos), com o detalhe de cada mutante gerado, status
  (`KILLED`/`SURVIVED`/`NO_COVERAGE`) e qual teste o matou.

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
