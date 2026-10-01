package io.continuum.testsupport.spec;

import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Board GWT for an automation with a private read model (a "todo list"), run directly against its
 * pure policy. As on the board, the LAST given event is the trigger; every event before it is
 * folded into the read model first:
 * <pre>
 * SPEC.given(setupEvent, ..., triggerEvent)   // setup folded with evolve, trigger handed to react
 *     .then(command, ...)                     // | .thenNothing()
 * </pre>
 */
public final class ReadModelAutomationSpecification<M, E, C> {

    private final M initial;
    private final BiFunction<M, ? super E, M> evolve;
    private final BiFunction<M, ? super E, ? extends List<? extends C>> react;

    private ReadModelAutomationSpecification(
            M initial, BiFunction<M, ? super E, M> evolve, BiFunction<M, ? super E, ? extends List<? extends C>> react) {
        this.initial = initial;
        this.evolve = evolve;
        this.react = react;
    }

    public static <M, E, C> ReadModelAutomationSpecification<M, E, C> of(
            M initial, BiFunction<M, ? super E, M> evolve, BiFunction<M, ? super E, ? extends List<? extends C>> react) {
        return new ReadModelAutomationSpecification<>(initial, evolve, react);
    }

    @SafeVarargs
    public final Then given(E... events) {
        if (events.length == 0) {
            throw new IllegalArgumentException("given(...) needs at least the trigger event - the last one");
        }
        M model = initial;
        for (int i = 0; i < events.length - 1; i++) {
            model = evolve.apply(model, events[i]);
        }
        return new Then(react.apply(model, events[events.length - 1]));
    }

    public final class Then {
        private final List<? extends C> commands;

        private Then(List<? extends C> commands) {
            this.commands = commands;
        }

        /** The policy dispatches exactly these commands, in this order. */
        @SafeVarargs
        public final void then(C... expected) {
            assertThat(commands).isEqualTo(List.of(expected));
        }

        /** The policy does not react. */
        public void thenNothing() {
            assertThat(commands).isEmpty();
        }
    }
}
