package io.continuum.testsupport.spec;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Board GWT for an automation's functional core (its policy), run directly against its pure
 * function:
 * <pre>
 * SPEC.given(triggerEvent)          // handed to the policy's react
 *     .then(command, ...)           // | .thenNothing()
 * </pre>
 */
public final class AutomationSpecification<T, C> {

    private final Function<T, ? extends List<? extends C>> react;

    private AutomationSpecification(Function<T, ? extends List<? extends C>> react) {
        this.react = react;
    }

    public static <T, C> AutomationSpecification<T, C> of(Function<T, ? extends List<? extends C>> react) {
        return new AutomationSpecification<>(react);
    }

    public Then given(T trigger) {
        return new Then(trigger);
    }

    public final class Then {
        private final T trigger;

        private Then(T trigger) {
            this.trigger = trigger;
        }

        /** The policy dispatches exactly these commands, in this order. */
        @SafeVarargs
        public final void then(C... expected) {
            assertThat(react.apply(trigger)).isEqualTo(List.of(expected));
        }

        /** The policy does not react. */
        public void thenNothing() {
            assertThat(react.apply(trigger)).isEmpty();
        }
    }
}
