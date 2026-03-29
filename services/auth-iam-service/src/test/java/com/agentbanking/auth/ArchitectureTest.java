package com.agentbanking.auth;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.agentbanking.auth");
    }

    @Test
    void domainModelShouldHaveNoFrameworkImports() {
        noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.transaction..",
                        "org.apache.kafka.."
                )
                .because("domain model must be framework-agnostic")
                .check(importedClasses);
    }

    @Test
    void domainModelShouldNotUseEntityAnnotations() {
        noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .orShould().beAnnotatedWith("jakarta.persistence.Table")
                .orShould().beAnnotatedWith("jakarta.persistence.Column")
                .because("domain models are records, not JPA entities")
                .check(importedClasses);
    }

    @Test
    void domainPortShouldHaveNoJpaOrKafkaImports() {
        noClasses()
                .that().resideInAPackage("..domain.port..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..",
                        "jakarta.transaction..",
                        "org.apache.kafka.."
                )
                .because("domain ports must not depend on JPA or Kafka directly")
                .check(importedClasses);
    }

    @Test
    void domainServiceShouldHaveNoJpaOrKafkaImports() {
        noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..",
                        "jakarta.transaction..",
                        "org.apache.kafka.."
                )
                .because("domain services must not depend on JPA or Kafka directly")
                .check(importedClasses);
    }

    @Test
    void domainServicesShouldNotUseServiceAnnotation() {
        noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("domain services must not use @Service, register via @Bean in DomainServiceConfig instead")
                .check(importedClasses);
    }

    @Test
    void repositoryImplementationsShouldBeInInfrastructureWithRepositoryAnnotation() {
        classes()
                .that().haveSimpleNameEndingWith("RepositoryImpl")
                .should().resideInAPackage("..infrastructure.persistence..")
                .andShould().beAnnotatedWith("org.springframework.stereotype.Repository")
                .because("repository implementations belong in infrastructure with @Repository")
                .check(importedClasses);
    }

    @Test
    void controllersShouldResideInInfrastructureWeb() {
        classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAPackage("..infrastructure.web..")
                .because("REST controllers must be in the infrastructure.web layer")
                .check(importedClasses);
    }

    @Test
    void useCaseImplementationsShouldResideInApplicationLayer() {
        classes()
                .that().haveSimpleNameEndingWith("UseCaseImpl")
                .should().resideInAPackage("..application.usecase..")
                .because("use case implementations belong in the application layer")
                .check(importedClasses);
    }

    @Test
    void configClassesShouldBeAnnotatedWithConfiguration() {
        classes()
                .that().haveSimpleNameEndingWith("Config")
                .and().resideInAPackage("..infrastructure.config..")
                .should().beAnnotatedWith("org.springframework.context.annotation.Configuration")
                .because("config classes must use @Configuration")
                .check(importedClasses);
    }

    @Test
    void useCaseImplementationsShouldNotBeAnnotatedWithService() {
        noClasses()
                .that().resideInAPackage("..application.usecase..")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("use case implementations must be registered via @Bean in config, not @Service")
                .check(importedClasses);
    }

    @Test
    void persistenceEntitiesShouldResideInInfrastructure() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..infrastructure.persistence..")
                .because("JPA entities must be in infrastructure.persistence layer")
                .check(importedClasses);
    }

    @Test
    void domainServicesAreRegisteredViaBeanConfig() {
        // Verify DomainServiceConfig exists and is annotated with @Configuration
        // Combined with domainServicesShouldNotUseServiceAnnotation test,
        // this ensures domain services are registered as beans, not via @Service
        classes()
                .that().haveFullyQualifiedName("com.agentbanking.auth.infrastructure.config.DomainServiceConfig")
                .should().beAnnotatedWith("org.springframework.context.annotation.Configuration")
                .check(importedClasses);
    }
}
