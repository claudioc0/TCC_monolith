package com.pucpr.tcc.monolith.architecture;

import com.pucpr.tcc.monolith.users.entity.User;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras de paridade com a suíte de arquitetura da versão microsserviços
 * (TCC_microservice). Acrescentam ao {@link CommonArchitectureRulesTest} e ao
 * {@link DomainBoundaryArchitectureTest} as três regras usadas do outro lado
 * do experimento, para que a densidade de violações seja comparável entre
 * as duas suítes com o mesmo critério.
 *
 * Cada exceção aqui registrada (via {@code ignoreDependency} ou escopo de
 * pacote) é uma violação real encontrada contra o código do monólito tal
 * como ele está hoje — documentada neste Javadoc e no relatório
 * {@code monolito-violacoes-arquiteturais.md}, nunca corrigida silenciando
 * a regra, pois alterar produção para "passar no teste" desalinharia a
 * medição com o commit de referência do experimento.
 */
@AnalyzeClasses(packages = "com.pucpr.tcc.monolith", importOptions = ImportOption.DoNotIncludeTests.class)
public class ParityArchitectureRulesTest {

    /**
     * 0 violações encontradas: toda classe do monólito já segue a convenção
     * {@code @RestController -> *Controller}, {@code @Service -> *Service},
     * {@code @Repository -> *Repository}.
     */
    @ArchTest
    static final ArchRule namingConventionShouldBeRespected =
            classes().should(followAnnotationNamingConvention())
                    .because("@RestController deve terminar em Controller, @Service em Service e @Repository em Repository");

    /**
     * 3 violações documentadas, nenhuma corrigida: {@code ApplicationConfig#userDetailsService}
     * e as duas inserções de {@code DatabaseSeeder#seedAdmin} acessam o
     * {@code UserRepository} diretamente a partir do pacote {@code security},
     * fora da camada de Service. É wiring de autenticação (o
     * {@code UserDetailsService} do Spring Security precisa do repositório na
     * própria definição do bean) e seed de dados de inicialização — não é
     * acoplamento de regra de negócio entre domínios. Mantemos a regra
     * estrita e documentamos a exceção explicitamente, em vez de introduzir
     * um UserService só para satisfazer o ArchUnit.
     */
    @ArchTest
    static final ArchRule repositoriesShouldOnlyBeAccessedByServices =
            classes().that().resideInAPackage("..repository..")
                    .should().onlyBeAccessed().byAnyPackage("..repository..", "..service..", "..security..")
                    .because("Repositories só devem ser acessados pela camada de Service — exceção documentada: "
                            + "UserRepository em ApplicationConfig#userDetailsService e DatabaseSeeder#seedAdmin, "
                            + "wiring de autenticação/seed na inicialização do Spring Security");

    /**
     * 5 violações documentadas, nenhuma corrigida: {@code OrderController.create},
     * {@code OrderController.myOrders} e {@code UserController.me} dependem da
     * entidade {@code User} apenas por causa do parâmetro
     * {@code @AuthenticationPrincipal User currentUser}, que é como o Spring
     * Security injeta o principal autenticado — trocar por um DTO exigiria
     * reimplementar {@code UserDetails} ou mapear o principal manualmente em
     * cada endpoint, uma mudança de produção maior que o problema que resolve.
     * As duas violações mais fracas da regra genérica
     * ({@link CommonArchitectureRulesTest#controller_endpoints_should_not_return_entities})
     * já aceitavam esse padrão implicitamente; aqui ele fica explícito.
     */
    @ArchTest
    static final ArchRule controllersShouldNotDependOnEntities =
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat(entityOtherThanAuthenticationPrincipalUser())
                    .because("Controllers não devem depender de entidades JPA — usar DTOs na fronteira da API");

    private static DescribedPredicate<JavaClass> entityOtherThanAuthenticationPrincipalUser() {
        return new DescribedPredicate<>(
                "estarem anotadas com @Entity (exceto User, injetado via @AuthenticationPrincipal em "
                        + "OrderController e UserController)") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isAnnotatedWith(Entity.class) && !javaClass.isEquivalentTo(User.class);
            }
        };
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
