package io.continuum.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * {@code slices/blueprint} is excluded from the runtime jar (maven-jar-plugin in pom.xml), so no
 * other production code may depend on it - such a class would compile and pass every test, then
 * fail with {@code ClassNotFoundException} in the shipped jar.
 */
class BlueprintIsolationTest {

    static final String BLUEPRINT = "io.continuum.slices.blueprint..";

    static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.continuum");

    @Test
    @DisplayName("no production code outside the blueprint depends on it - it isn't in the runtime jar")
    void nothingDependsOnTheBlueprint() {
        noClasses().that().resideOutsideOfPackage(BLUEPRINT)
                .should().dependOnClassesThat().resideInAPackage(BLUEPRINT)
                .because("slices/blueprint is a reference for the build kit and is excluded from the runtime jar")
                .check(PRODUCTION);
    }
}
