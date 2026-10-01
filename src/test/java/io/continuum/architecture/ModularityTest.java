package io.continuum.architecture;

import io.continuum.ContinuumApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModularityTest {

    static final ApplicationModules MODULES = ApplicationModules.of(ContinuumApplication.class);

    @Test
    @DisplayName("every slices/<context> package is its own application module")
    void contextsAreModules() {
        assertThat(MODULES.stream().map(module -> module.getBasePackage().getName()))
                .contains("io.continuum.slices.blueprint");
    }

    @Test
    @DisplayName("every context publishes its events package - and only that - as named interface 'events'")
    void contextsPublishTheirEvents() {
        assertThat(sliceContexts()).allSatisfy(context -> {
            var events = context.getNamedInterfaces().getByName("events");
            assertThat(events).as("named interface 'events' of %s", context.getBasePackage().getName()).isPresent();
            assertThat(events.get().asJavaClasses())
                    .allSatisfy(type -> assertThat(type.getPackageName()).endsWith(".events"));
        });
    }

    @Test
    @DisplayName("modules only use each other's API, without cycles")
    void verifiesModuleBoundaries() {
        MODULES.verify();
    }

    private static List<ApplicationModule> sliceContexts() {
        return MODULES.stream()
                .filter(module -> module.getBasePackage().getName().startsWith("io.continuum.slices."))
                .toList();
    }
}
