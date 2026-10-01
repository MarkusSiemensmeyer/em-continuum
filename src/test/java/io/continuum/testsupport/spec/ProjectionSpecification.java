package io.continuum.testsupport.spec;

import java.util.Optional;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Board GWT for a state-view slice's functional core (its projection), run directly against its
 * pure function - one read-model row, folded from no row at all:
 * <pre>
 * SPEC.given(event, ...)            // folded with the projection's evolve
 *     .then(expectedRow)            // | .thenNothing()
 * </pre>
 */
public final class ProjectionSpecification<V, E> {

    private final BiFunction<Optional<V>, ? super E, Optional<V>> evolve;

    private ProjectionSpecification(BiFunction<Optional<V>, ? super E, Optional<V>> evolve) {
        this.evolve = evolve;
    }

    public static <V, E> ProjectionSpecification<V, E> of(BiFunction<Optional<V>, ? super E, Optional<V>> evolve) {
        return new ProjectionSpecification<>(evolve);
    }

    @SafeVarargs
    public final Then given(E... events) {
        Optional<V> row = Optional.empty();
        for (E event : events) {
            row = evolve.apply(row, event);
        }
        return new Then(row);
    }

    public final class Then {
        private final Optional<V> row;

        private Then(Optional<V> row) {
            this.row = row;
        }

        /** The read model shows exactly this row. */
        public void then(V expected) {
            assertThat(row).contains(expected);
        }

        /** The read model shows no row. */
        public void thenNothing() {
            assertThat(row).isEmpty();
        }
    }
}
