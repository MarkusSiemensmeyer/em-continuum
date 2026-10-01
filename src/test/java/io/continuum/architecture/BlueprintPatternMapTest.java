package io.continuum.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.continuum.slices.EventModelingPattern;
import io.continuum.slices.EventModelingPattern.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static io.continuum.slices.EventModelingPattern.Type.AUTOMATION;
import static io.continuum.slices.EventModelingPattern.Type.STATE_CHANGE;
import static io.continuum.slices.EventModelingPattern.Type.STATE_VIEW;
import static io.continuum.slices.EventModelingPattern.Type.TRANSLATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the blueprint's pattern map honest: which slice package is the example for which Event
 * Modeling pattern - the table in {@code slices/blueprint/package-info.java}, declared per slice
 * with {@link EventModelingPattern}.
 */
class BlueprintPatternMapTest {

    static final String BLUEPRINT = "io.continuum.slices.blueprint";

    /** The table in blueprint/package-info.java, as data - slice package (relative) → pattern. */
    static final Map<String, Type> EXPECTED = Map.of(
            "registeritem", STATE_CHANGE,
            "activateitem", STATE_CHANGE,
            "openlocation", STATE_CHANGE,
            "items", STATE_VIEW,
            "automation.activateregistereditem", AUTOMATION,
            "automation.activateitemsatopenedlocation", AUTOMATION,
            "translation.facilitysitestatus", TRANSLATION,
            "translation.reportitemtoassetregistry", TRANSLATION);

    /** Every package holding a {@code funcore} sub-package is a slice. */
    static final Set<String> SLICES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BLUEPRINT)
            .stream()
            .map(JavaClass::getPackageName)
            .filter(name -> name.endsWith(".funcore"))
            .map(name -> name.substring(BLUEPRINT.length() + 1, name.length() - ".funcore".length()))
            .collect(Collectors.toSet());

    @Test
    @DisplayName("every blueprint slice declares its Event Modeling pattern, exactly as in the table")
    void everySliceDeclaresItsPattern() {
        var declared = new TreeMap<String, Type>();
        SLICES.forEach(slice -> declared.put(slice, patternOf(slice).map(EventModelingPattern::value).orElse(null)));

        assertThat(declared).containsExactlyInAnyOrderEntriesOf(EXPECTED);
    }

    @Test
    @DisplayName("every Event Modeling pattern has at least one example in the blueprint")
    void everyPatternHasAnExample() {
        assertThat(EXPECTED.values()).containsAll(Arrays.asList(Type.values()));
    }

    @Test
    @DisplayName("every declaration says what its example shows (variant)")
    void everyDeclarationHasAVariant() {
        assertThat(SLICES).allSatisfy(slice ->
                assertThat(patternOf(slice)).hasValueSatisfying(pattern -> assertThat(pattern.variant()).isNotBlank()));
    }

    private static Optional<EventModelingPattern> patternOf(String slice) {
        try {
            var packageInfo = Class.forName(BLUEPRINT + "." + slice + ".package-info");
            return Optional.ofNullable(packageInfo.getAnnotation(EventModelingPattern.class));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }
}
