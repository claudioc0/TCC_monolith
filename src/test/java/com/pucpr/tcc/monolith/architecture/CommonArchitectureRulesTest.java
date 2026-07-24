package com.pucpr.tcc.monolith.architecture;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras de arquitetura comuns a qualquer domínio do monólito, conforme
 * o protocolo GQM do experimento (Controller -> Service -> Repository -> Entity,
 * com DTOs na fronteira da API).
 */
@AnalyzeClasses(packages = "com.pucpr.tcc.monolith", importOptions = ImportOption.DoNotIncludeTests.class)
public class CommonArchitectureRulesTest {

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories_directly =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("Controllers devem acessar o domínio apenas através da camada de Service");

    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers =
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Services não podem depender da camada de apresentação");

    @ArchTest
    static final ArchRule controller_endpoints_should_not_return_entities =
            methods().that().areDeclaredInClassesThat().resideInAPackage("..controller..")
                    .and().arePublic()
                    .should(notExposeJpaEntityAsReturnType())
                    .because("Entidades JPA nunca devem ser retornadas por um Controller — use DTOs");

    /**
     * Inspeciona o tipo genérico real de retorno (ex.: o T de ResponseEntity&lt;T&gt;)
     * via reflection, pois o ArchUnit por padrão só enxerga o tipo apagado (raw type).
     */
    private static ArchCondition<JavaMethod> notExposeJpaEntityAsReturnType() {
        return new ArchCondition<>("retornar um DTO, nunca uma entidade @Entity") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Type genericReturnType = method.reflect().getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType parameterizedType) {
                    for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                        if (typeArgument instanceof Class<?> rawTypeArgument
                                && rawTypeArgument.isAnnotationPresent(Entity.class)) {
                            String message = String.format(
                                    "Método %s retorna a entidade %s diretamente (deveria retornar um DTO)",
                                    method.getFullName(), rawTypeArgument.getSimpleName());
                            events.add(SimpleConditionEvent.violated(method, message));
                        }
                    }
                }
            }
        };
    }
}
