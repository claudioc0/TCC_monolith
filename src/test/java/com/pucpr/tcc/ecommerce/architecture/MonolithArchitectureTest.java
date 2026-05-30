package com.pucpr.tcc.ecommerce.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit global scan of the entire monolith.
 * This single execution covers all three bounded contexts simultaneously.
 * The total time of this scan is the RQ2 metric for the monolith side.
 */
class MonolithArchitectureTest {

    // Single import: ArchUnit scans the ENTIRE project in one pass (RQ2 measurement point)
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.pucpr.tcc.ecommerce");
    }

    // ── PRODUCT context ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[product] Domínio não deve depender de infraestrutura")
    void productDomainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..product.domain..")
                .should().dependOnClassesThat().resideInAPackage("..product.infrastructure..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[product] Domínio não deve depender de Spring")
    void productDomainShouldNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..product.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[product] Application não deve depender de infraestrutura")
    void productApplicationShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..product.application..")
                .should().dependOnClassesThat().resideInAPackage("..product.infrastructure..");
        rule.check(importedClasses);
    }

    // ── CUSTOMER context ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[customer] Domínio não deve depender de infraestrutura")
    void customerDomainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..customer.domain..")
                .should().dependOnClassesThat().resideInAPackage("..customer.infrastructure..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[customer] Domínio não deve depender de Spring")
    void customerDomainShouldNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..customer.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[customer] Application não deve depender de infraestrutura")
    void customerApplicationShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..customer.application..")
                .should().dependOnClassesThat().resideInAPackage("..customer.infrastructure..");
        rule.check(importedClasses);
    }

    // ── ORDER context ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[order] Domínio não deve depender de infraestrutura")
    void orderDomainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..order.domain..")
                .should().dependOnClassesThat().resideInAPackage("..order.infrastructure..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[order] Domínio não deve depender de Spring")
    void orderDomainShouldNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..order.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[order] Application não deve depender de infraestrutura")
    void orderApplicationShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..order.application..")
                .should().dependOnClassesThat().resideInAPackage("..order.infrastructure..");
        rule.check(importedClasses);
    }

    // ── CROSS-CONTEXT: order application may use customer/product APPLICATION layer ──

    @Test
    @DisplayName("[cross] Order application pode depender de customer e product application")
    void orderApplicationMayDependOnOtherApplicationLayers() {
        // This is valid in the monolith — cross-context via in-memory DI
        ArchRule rule = classes()
                .that().resideInAPackage("..order.application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..order.domain..",
                        "..order.application..",
                        "..product.domain..",
                        "..product.application..",
                        "..customer.domain..",
                        "..customer.application..",
                        "org.springframework..",
                        "java..",
                        "jakarta.."
                );
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[cross] Order domain NÃO deve depender de outros contextos")
    void orderDomainShouldNotDependOnOtherContexts() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..order.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..product..", "..customer..");
        rule.check(importedClasses);
    }

    // ── GLOBAL: controllers and repositories must be in infrastructure ──

    @Test
    @DisplayName("[global] Controllers devem residir na infraestrutura")
    void controllersShouldResideInInfrastructure() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..infrastructure..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[global] JpaRepositories devem residir na infraestrutura")
    void jpaRepositoriesShouldResideInInfrastructure() {
        ArchRule rule = classes()
                .that().haveSimpleNameStartingWith("Jpa")
                .should().resideInAPackage("..infrastructure..");
        rule.check(importedClasses);
    }
}
