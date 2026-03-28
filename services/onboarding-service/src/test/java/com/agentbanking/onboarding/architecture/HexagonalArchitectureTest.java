package com.agentbanking.onboarding.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(packages = "com.agentbanking.onboarding")
class HexagonalArchitectureTest {

    @ArchTest
    void domainLayerMustNotContainJpaAnnotations(JavaClasses classes) {
        com.tngtech.archunit.lang.ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition
            .classes().that().resideInAnyPackage("..domain..")
            .should().notBeAnnotatedWith(Entity.class)
            .andShould().notBeAnnotatedWith(Table.class)
            .andShould().notBeAnnotatedWith(Column.class)
            .andShould().notBeAnnotatedWith(Id.class);
        rule.check(classes);
    }

    @ArchTest
    void domainLayerMustNotContainSpringAnnotations(JavaClasses classes) {
        com.tngtech.archunit.lang.ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition
            .classes().that().resideInAnyPackage("..domain..")
            .should().notBeAnnotatedWith(Service.class)
            .andShould().notBeAnnotatedWith(Repository.class)
            .andShould().notBeAnnotatedWith(Component.class)
            .andShould().notBeAnnotatedWith(Transactional.class);
        rule.check(classes);
    }

    @ArchTest
    void domainLayerMustNotUseEntityManager(JavaClasses classes) {
        com.tngtech.archunit.lang.ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition
            .noClasses().that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .areAssignableFrom(EntityManager.class);
        rule.check(classes);
    }

    @ArchTest
    void domainLayerMustOnlyAccessPorts(JavaClasses classes) {
        com.tngtech.archunit.lang.ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition
            .classes().that().resideInAnyPackage("..domain.service..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "com.agentbanking.common..", "java..", "jakarta..", "org.junit..", "org.mockito..");
        rule.check(classes);
    }
}
