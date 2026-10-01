package io.continuum.testsupport.spec;

import java.util.Optional;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Board GWT for an OUTBOUND translation's functional core, run directly against its pure functions:
 * <pre>
 * SPEC.given(event, ...)            // facts folded with the core's evolve (e.g. "already reported")
 *     .when(triggerEvent)           // our event, handed to the core's translate
 *     .then(outcome)                // what goes to the external system (+ the fact to record) | .thenNothing()
 * </pre>
 * Inbound translations (external message → our commands) have a stateless automation's shape and
 * use {@link AutomationSpecification}.
 */
public final class TranslationSpecification<S, E, T, O> {

    private final S initial;
    private final BiFunction<S, ? super E, S> evolve;
    private final BiFunction<S, T, Optional<O>> translate;

    private TranslationSpecification(S initial, BiFunction<S, ? super E, S> evolve, BiFunction<S, T, Optional<O>> translate) {
        this.initial = initial;
        this.evolve = evolve;
        this.translate = translate;
    }

    public static <S, E, T, O> TranslationSpecification<S, E, T, O> of(
            S initial, BiFunction<S, ? super E, S> evolve, BiFunction<S, T, Optional<O>> translate) {
        return new TranslationSpecification<>(initial, evolve, translate);
    }

    @SafeVarargs
    public final When given(E... events) {
        S state = initial;
        for (E event : events) {
            state = evolve.apply(state, event);
        }
        return new When(state);
    }

    public final class When {
        private final S state;

        private When(S state) {
            this.state = state;
        }

        public Then when(T trigger) {
            return new Then(translate.apply(state, trigger));
        }
    }

    public final class Then {
        private final Optional<O> outcome;

        private Then(Optional<O> outcome) {
            this.outcome = outcome;
        }

        /** The translation produces exactly this outcome. */
        public void then(O expected) {
            assertThat(outcome).contains(expected);
        }

        /** The translation sends nothing. */
        public void thenNothing() {
            assertThat(outcome).isEmpty();
        }
    }
}
