package io.continuum.architecture;

import org.springframework.modulith.core.ApplicationModuleDetectionStrategy;
import org.springframework.modulith.core.JavaPackage;

import java.util.stream.Stream;

/**
 * Makes every {@code slices/<context>} package its own Spring Modulith application module - the
 * default strategy would only see {@code io.continuum.slices} as one module holding all contexts.
 * The other direct sub-packages ({@code eventstore}, {@code config}) stay modules as usual.
 * <p>
 * Registered via {@code spring.modulith.detection-strategy} in the test
 * {@code application.properties} - Modulith is a test-only dependency, used for verification.
 */
public class SliceContextModuleDetection implements ApplicationModuleDetectionStrategy {

    static final String SLICES = "slices";

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage basePackage) {
        return basePackage.getDirectSubPackages().stream()
                .flatMap(pkg -> pkg.getLocalName().equals(SLICES)
                        ? pkg.getDirectSubPackages().stream()
                        : Stream.of(pkg));
    }
}
