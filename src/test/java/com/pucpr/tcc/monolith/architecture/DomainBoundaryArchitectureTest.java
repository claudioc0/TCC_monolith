package com.pucpr.tcc.monolith.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras de fronteira entre os pacotes de domínio (orders, products, users, reports).
 *
 * No Monólito essa fronteira é garantida por convenção/injeção de interfaces (Service),
 * já que fisicamente nada impede o acesso direto; nos Microsserviços a mesma fronteira
 * é imposta pela rede (cada domínio só é alcançável via HTTP). Aqui medimos a densidade
 * de violações dessa convenção.
 *
 * Escopo: aplica-se às camadas service/repository de cada domínio — a camada de
 * controller é excluída propositalmente porque `@AuthenticationPrincipal User` é uma
 * integração de framework (Spring Security), não um acoplamento de negócio entre domínios.
 */
@AnalyzeClasses(packages = "com.pucpr.tcc.monolith", importOptions = ImportOption.DoNotIncludeTests.class)
public class DomainBoundaryArchitectureTest {

    private static final String[] OTHER_THAN_ORDERS = {
            "..products.entity..", "..products.repository..",
            "..users.entity..", "..users.repository..",
            "..reports.entity..", "..reports.repository.."
    };

    private static final String[] OTHER_THAN_PRODUCTS = {
            "..orders.entity..", "..orders.repository..",
            "..users.entity..", "..users.repository..",
            "..reports.entity..", "..reports.repository.."
    };

    private static final String[] OTHER_THAN_USERS = {
            "..orders.entity..", "..orders.repository..",
            "..products.entity..", "..products.repository..",
            "..reports.entity..", "..reports.repository.."
    };

    private static final String[] OTHER_THAN_REPORTS = {
            "..orders.entity..", "..orders.repository..",
            "..products.entity..", "..products.repository..",
            "..users.entity..", "..users.repository.."
    };

    @ArchTest
    static final ArchRule orders_should_not_access_other_domains_internals =
            noClasses().that().resideInAnyPackage("..orders.service..", "..orders.repository..")
                    .should().dependOnClassesThat().resideInAnyPackage(OTHER_THAN_ORDERS)
                    .because("orders deve acessar outros domínios apenas via Service (ex.: ProductService)");

    @ArchTest
    static final ArchRule products_should_not_access_other_domains_internals =
            noClasses().that().resideInAnyPackage("..products.service..", "..products.repository..")
                    .should().dependOnClassesThat().resideInAnyPackage(OTHER_THAN_PRODUCTS)
                    .because("products não deve conhecer a implementação interna de outros domínios");

    @ArchTest
    static final ArchRule users_should_not_access_other_domains_internals =
            noClasses().that().resideInAnyPackage("..users.service..", "..users.repository..")
                    .should().dependOnClassesThat().resideInAnyPackage(OTHER_THAN_USERS)
                    .because("users não deve conhecer a implementação interna de outros domínios");

    @ArchTest
    static final ArchRule reports_should_not_access_other_domains_internals =
            noClasses().that().resideInAnyPackage("..reports.service..", "..reports.repository..")
                    .should().dependOnClassesThat().resideInAnyPackage(OTHER_THAN_REPORTS)
                    .because("reports deve agregar dados de outros domínios via Service, não via Repository direto");
}
