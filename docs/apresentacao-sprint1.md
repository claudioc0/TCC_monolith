# Apresentação — Sprint 1 (1ª Avaliação Somativa)

Roteiro de pontos principais para a apresentação ao orientador. Sprint 1 =
50% da execução do projeto: entrega da versão monolítica do sistema, sua
suíte de testes, e a execução/medição das métricas do protocolo GQM.

## 1. O que foi entregue

- **Sistema monolítico completo** — Spring Boot 3.2, JDK 21, H2, Spring
  Security + JWT. Builda, gera jar executável, roda (API + frontend de
  demonstração em `index.html`).
- **Suíte de testes: 98/98, 0 falhas** — 69 testes de unidade (JUnit 5 +
  Mockito) + 29 de integração (`@SpringBootTest`) + 7 regras de arquitetura
  executáveis (ArchUnit).
- **Quatro métricas do protocolo GQM medidas e documentadas**, cada uma
  com relatório próprio em `docs/reports/`.
- **Repositório organizado**, histórico limpo, Maven Wrapper incluído
  (`./mvnw`) — roda em qualquer máquina só com JDK 21 instalado.

## 2. As quatro métricas (estado atual)

| Métrica | Ferramenta | Resultado | O que ela responde |
|---|---|---|---|
| Branch Coverage | JaCoCo 0.8.11 | **81,0%** (34/42 ramos) | A suíte exercita a lógica condicional do sistema? |
| Mutation Score | PITest 1.15.3 | **80,6%** (137/170, Test Strength 95,8%) | Quando exercita, a suíte *detectaria* um defeito de verdade? |
| Violações Arquiteturais | ArchUnit 1.2.1 | **0/7 regras** | As fronteiras de domínio declaradas são respeitadas pelo código? |
| Acoplamento (CBO) | `ck` (Aniche et al.) | **7,45** (monólito) vs. **6,71** (microsserviços) | Quanto esforço de isolamento (mock) um teste de unidade precisa? |

Ambiente de referência: **JDK 21.0.12.1**, determinístico (verificado por
hash MD5 do CSV do JaCoCo e comparação mutante a mutante do PITest em
múltiplas rodadas).

## 3. O que demonstra rigor metodológico (o ponto mais forte da apresentação)

Três erros/vieses reais foram **encontrados e corrigidos durante o
projeto**, e o processo de correção ficou registrado, não escondido:

1. **Branch coverage estava errado por 9 pontos percentuais** — uma
   medição anterior lia a coluna de instruções de bytecode do CSV do
   JaCoCo como se fosse a de ramos (90,1% relatado vs. 81,0% real). Achado
   por verificação independente de um colega (Eric), corrigido e
   documentado com a causa raiz.
2. **Um gap de teste real escapou da suíte** — `OrderStatus.CONFIRMADO`
   aceitava qualquer transição sem nenhum teste reclamar. Achado pelo
   PITest na verificação independente, fechado com dois testes novos.
3. **O mutation score estava artificialmente inflado** — uma exclusão de
   classes de bootstrap (`DatabaseSeeder` etc.) do escopo do PITest
   escondia 3 mutantes sobreviventes reais, inflando o score em 1,5 p.p.
   Medido o efeito antes de decidir remover a exclusão.

**Mensagem para a banca:** o valor do processo não é "zero erro", é
"todo erro encontrado foi medido, corrigido e documentado explicitamente"
— inclusive quando o número piorou depois da correção.

## 4. Verificação independente e reprodutibilidade

- Medição própria + verificação independente de um colega (Eric, ambiente
  JDK 21 desde o início) + uma terceira reprodução em máquina separada
  (Vinicius, 2026-09-12) — **os três convergem no mesmo resultado**, mutante
  a mutante, depois do ambiente ser padronizado.
- Determinismo verificado empiricamente (não assumido): mesma suíte, mesma
  configuração, resultado idêntico em repetições.

## 5. Resposta a pedido do orientador: padronização de ambiente

- O orientador pediu para padronizar o JDK usado entre monólito e
  microsserviços em **JDK 21** (o que o Eric já usava).
- Feito: `pom.xml` migrado, suíte inteira revalidada, métricas
  recalculadas. Mutation score mudou de 82,5% → 80,6% — diferença
  explicada e documentada (bytecode gerado por compiladores de JDKs
  diferentes), não uma regressão real de qualidade.

## 6. Ameaças à validade — todas resolvidas dos dois lados (2026-09-13)

| Ameaça | Status |
|---|---|
| Versão do PITest divergente entre monólito/microsserviços | ✅ Resolvida |
| Exclusão de classes no escopo do PITest | ✅ Resolvida |
| Divergência de JDK | ✅ Resolvida — os dois lados declaram e usam JDK 21 |
| CBO comparativo | ✅ Resolvida — 7,45 (monólito) vs. 6,71 (microsserviços) |
| Assimetria de esforço de teste (4 integração aqui, 0 relatado lá) | ✅ Resolvida — microsserviços escreveu os 4 testes equivalentes |

**Mensagem para a banca:** nenhuma ameaça à validade conhecida ficou em
aberto na entrega da Sprint 1 — cada uma foi fechada com uma ação
verificável, registrada dos dois lados (`pendencias-microservicos.md` →
`FECHAMENTO_PENDENCIAS-microservicos.md`).

## 7. Próximos passos (Sprint 2)

- Testes de integração para `UserController`/`ProductController` (pacotes
  com mutation score mais baixo).
- Capítulo de comparação estatística final monólito vs. microsserviços,
  agora que as quatro métricas têm par dos dois lados.
- Repetir a verificação independente (dupla checagem) também sobre os
  números novos de microsserviços, para manter o mesmo padrão de rigor
  aplicado ao monólito.

## 8. Onde está tudo (para consulta durante a apresentação)

- **Ponto único de consulta:** [`docs/reports/resultados-finais.md`](./reports/resultados-finais.md)
- Relatórios detalhados por métrica: `cobertura-codigo.md`,
  `mutation-score.md`, `violacoes-arquiteturais.md`, `acoplamento-cbo.md`
- Log de execuções (histórico rodada a rodada): `execution-log.md`
- Documento navegável e visual, para projetar: [artifact publicado](https://claude.ai/code/artifact/eebc5cef-cbdf-4b91-9075-5eb370e4e46c)

## 9. Perguntas prováveis da banca (e a resposta curta)

- **"Por que só 81% de branch coverage, não 100%?"** — o sistema tem só 42
  ramos reais; a métrica satura rápido e discrimina pouco aqui. É por isso
  que o protocolo usa quatro métricas, não uma só — mutation score e CBO
  discriminam melhor.
- **"Por que o mutation score caiu depois da migração de JDK?"** —
  compiladores diferentes geram bytecode ligeiramente diferente para o
  mesmo código-fonte; 4 mutantes a menos, mesmos 6 sobreviventes, nenhuma
  conclusão qualitativa muda.
- **"Os 6 mutantes sobreviventes são um problema?"** — 3 são código de
  *wiring* de framework (categoria aceita como residual na literatura); os
  outros 3 (`DatabaseSeeder`) são teste faltando de baixo valor prático,
  documentados como decisão consciente, não descuido.
- **"Como vocês garantem que o número não é sorte de uma execução?"** —
  determinismo verificado por hash/comparação mutante a mutante em
  múltiplas rodadas, em três máquinas diferentes, com resultado idêntico.
- **"Microsserviços tem CBO menor (6,71 vs. 7,45) — isso prova que é mais
  desacoplado?"** — não sozinho. Parte da queda é troca de acoplamento
  Java direto por chamada HTTP (`ProductClient`/`OrderClient`), que o CBO
  não mede; e os denominadores (29 vs. 48 classes) não são diretamente
  comparáveis, porque a decomposição duplica infraestrutura por serviço.
  O dado por camada (`controller` com `controller`) é mais informativo
  que o agregado sozinho.
