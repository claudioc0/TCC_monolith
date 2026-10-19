package com.pucpr.tcc.monolith.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regras de paridade com a suíte de arquitetura da versão microsserviços
 * (TCC_microservice). Acrescentam ao {@link CommonArchitectureRulesTest} e ao
 * {@link DomainBoundaryArchitectureTest} as três regras usadas do outro lado
 * do experimento.
 *
 * <p>Diferença deliberada em relação às outras duas classes: esta NÃO usa
 * {@code @ArchTest} (que falha a suíte em qualquer violação, forçando a
 * tentação de afrouxar a regra até ela passar). As duas regras com violação
 * real usam {@link ArchRule#evaluate(JavaClasses)} e fixam, via
 * {@code assertThat(...).hasSize(N)}, o número exato de violações
 * conhecidas hoje. A regra em si permanece estrita e idêntica à usada nos
 * microsserviços — nenhuma exceção de escopo, nenhum pacote ou classe
 * excluído do predicado. Isso é o que torna o "0 violações" do lado de
 * microsserviços e o "N violações" daqui diretamente comparáveis: aqui N
 * não foi zerado afrouxando a regra, foi medido e fixado como dado.
 *
 * <p>Se uma violação nova aparecer (regressão) ou uma existente for
 * corrigida, o teste quebra — obriga atualizar este número conscientemente,
 * em vez de deixá-lo silenciosamente desatualizado ou escondido atrás de um
 * "0" artificial.
 */
class ParityArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.pucpr.tcc.monolith");
    }

    @Test
    @DisplayName("namingConventionShouldBeRespected — 0 violações")
    void namingConventionShouldBeRespected() {
        ArchRule rule = classes().should(followAnnotationNamingConvention())
                .because("@RestController deve terminar em Controller, @Service em Service e @Repository em Repository");

        EvaluationResult result = rule.evaluate(classes);

        assertThat(result.getFailureReport().getDetails()).isEmpty();
    }

    /**
     * 3 violações reais, documentadas, não corrigidas: {@code ApplicationConfig#userDetailsService}
     * e as duas inserções de {@code DatabaseSeeder#seedAdmin} acessam o
     * {@code UserRepository} diretamente a partir do pacote {@code security},
     * fora da camada de Service. É wiring de autenticação (o
     * {@code UserDetailsService} do Spring Security precisa do repositório na
     * própria definição do bean) e seed de dados de inicialização — não é
     * acoplamento de regra de negócio entre domínios. A regra fica estrita
     * (sem exceção de pacote embutida); o número conhecido de violações é
     * fixado aqui, não escondido.
     */
    @Test
    @DisplayName("repositoriesShouldOnlyBeAccessedByServices — 3 violações reais, documentadas (não corrigidas)")
    void repositoriesShouldOnlyBeAccessedByServices() {
        ArchRule rule = classes().that().resideInAPackage("..repository..")
                .should().onlyBeAccessed().byAnyPackage("..repository..", "..service..")
                .because("Repositories só devem ser acessados pela camada de Service");

        EvaluationResult result = rule.evaluate(classes);
        List<String> violations = result.getFailureReport().getDetails();

        assertThat(violations).hasSize(3);
        assertThat(violations).anyMatch(v -> v.contains("ApplicationConfig"));
        assertThat(violations).filteredOn(v -> v.contains("DatabaseSeeder")).hasSize(2);
    }

    /**
     * 5 violações reais, documentadas, não corrigidas: {@code OrderController.create},
     * {@code OrderController.myOrders} e {@code UserController.me} dependem da
     * entidade {@code User} apenas por causa do parâmetro
     * {@code @AuthenticationPrincipal User currentUser}, que é como o Spring
     * Security injeta o principal autenticado — trocar por um DTO exigiria
     * reimplementar {@code UserDetails} ou mapear o principal manualmente em
     * cada endpoint, uma mudança de produção maior que o problema que
     * resolve. A regra fica estrita (idêntica à versão usada nos
     * microsserviços, que foi desenhada propositalmente para acusar este
     * caso); o número conhecido de violações é fixado aqui, não escondido.
     */
    @Test
    @DisplayName("controllersShouldNotDependOnEntities — 5 violações reais, documentadas (não corrigidas)")
    void controllersShouldNotDependOnEntities() {
        ArchRule rule = noClasses().that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                .because("Controllers não devem depender de entidades JPA — usar DTOs na fronteira da API");

        EvaluationResult result = rule.evaluate(classes);
        List<String> violations = result.getFailureReport().getDetails();

        assertThat(violations).hasSize(5);
        assertThat(violations).allMatch(v -> v.contains("User"));
    }

    private static ArchCondition<JavaClass> followAnnotationNamingConvention() {
        return new ArchCondition<>("respeitar a convenção de nome por tipo de anotação") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                checkSuffix(javaClass, RestController.class, "Controller", events);
                checkSuffix(javaClass, Service.class, "Service", events);
                checkSuffix(javaClass, Repository.class, "Repository", events);
            }

            private void checkSuffix(JavaClass javaClass, Class<? extends java.lang.annotation.Annotation> annotation,
                                      String expectedSuffix, ConditionEvents events) {
                if (javaClass.isAnnotatedWith(annotation) && !javaClass.getSimpleName().endsWith(expectedSuffix)) {
                    String message = String.format(
                            "Classe %s está anotada com @%s mas não termina em \"%s\"",
                            javaClass.getFullName(), annotation.getSimpleName(), expectedSuffix);
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }
}
