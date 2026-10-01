package io.continuum.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Keeps every slice's {@code funcore} package a real functional core: plain Java over the
 * slice's own commands, events and core types - no I/O, no framework, no infrastructure.
 */
class FunctionalCoreTest {

    static final String FUNCORE = "io.continuum.slices..funcore..";

    static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.continuum");

    @Test
    @DisplayName("funcore only uses plain Java and the slices' own types")
    void onlyPlainJava() {
        classes().that().resideInAPackage(FUNCORE)
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "io.continuum.slices..")
                .because("a functional core is plain Java - the imperative shell does all I/O")
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("funcore never reaches infrastructure, not even through the types it uses")
    void noInfrastructureEvenTransitively() {
        noClasses().that().resideInAPackage(FUNCORE)
                .should().transitivelyDependOnClassesThat().resideInAnyPackage(
                        "io.umadb..",
                        "org.springframework..",
                        "jakarta..",
                        "tools.jackson..",
                        "com.fasterxml.jackson..",
                        "io.continuum.eventstore..",
                        "io.continuum.config..")
                .because("a command or event the core uses must not drag persistence or framework code in")
                .check(PRODUCTION);
    }
}
