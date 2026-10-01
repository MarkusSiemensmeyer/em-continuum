package io.continuum.testsupport.spec;

import io.continuum.slices.CommandRejectedException;

import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Board GWT for a state-change slice's functional core, run directly against its pure functions:
 * <pre>
 * SPEC.given(event, ...)            // folded with the core's evolve
 *     .when(command)                // handed to the core's decide
 *     .then(event, ...)             // | .thenNothing() | .thenRejected()
 * </pre>
 */
public final class DecisionSpecification<S, C, E> {

    private final S initial;
    private final BiFunction<S, ? super E, S> evolve;
    private final BiFunction<S, C, ? extends List<? extends E>> decide;

    private DecisionSpecification(S initial, BiFunction<S, ? super E, S> evolve, BiFunction<S, C, ? extends List<? extends E>> decide) {
        this.initial = initial;
        this.evolve = evolve;
        this.decide = decide;
    }

    public static <S, C, E> DecisionSpecification<S, C, E> of(
            S initial, BiFunction<S, ? super E, S> evolve, BiFunction<S, C, ? extends List<? extends E>> decide) {
        return new DecisionSpecification<>(initial, evolve, decide);
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

        public Then when(C command) {
            return new Then(state, command);
        }
    }

    public final class Then {
        private final S state;
        private final C command;

        private Then(S state, C command) {
            this.state = state;
            this.command = command;
        }

        /** The core decides exactly these events, in this order. */
        @SafeVarargs
        public final void then(E... expected) {
            assertThat(decide()).isEqualTo(List.of(expected));
        }

        /** The core decides nothing needs to happen - no events, no rejection. */
        public void thenNothing() {
            assertThat(decide()).isEmpty();
        }

        /** The core rejects the command - a business rule is violated. */
        public void thenRejected() {
            assertThatThrownBy(this::decide).isInstanceOf(CommandRejectedException.class);
        }

        private List<? extends E> decide() {
            return decide.apply(state, command);
        }
    }
}
