package com.agentbanking.orchestrator.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.agentbanking.orchestrator")
public class HexagonalArchitectureTest {

    @ArchTest
    void domainPortsShouldNotBeSpringComponents(JavaClasses classes) {
        noClasses().that().resideInAnyPackage("..domain.port..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .check(classes);
    }

    @ArchTest
    void domainServicesShouldNotHaveSpringAnnotations(JavaClasses classes) {
        noClasses().that().resideInAnyPackage("..domain.service..")
            .should()
            .beAnnotatedWith(org.springframework.stereotype.Service.class)
            .orShould()
            .beAnnotatedWith(org.springframework.stereotype.Component.class)
            .orShould()
            .beAnnotatedWith(org.springframework.stereotype.Repository.class)
            .check(classes);
    }
}
