package io.continuum.eventstore;

import io.continuum.slices.CommandRejectedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConflictRetryTest {

    @Test
    @DisplayName("given no conflict, runs the attempt exactly once")
    void noConflict() {
        var attempts = new AtomicInteger();

        ConflictRetry.onConflict(attempts::incrementAndGet);

        assertThat(attempts).hasValue(1);
    }

    @Test
    @DisplayName("given conflicts on the first attempts, re-runs until one succeeds")
    void conflictThenSuccess() {
        var attempts = new AtomicInteger();

        ConflictRetry.onConflict(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw conflict();
            }
        });

        assertThat(attempts).hasValue(3);
    }

    @Test
    @DisplayName("given a conflict on every attempt, gives up after 5 attempts and rethrows")
    void conflictEveryTime() {
        var attempts = new AtomicInteger();

        assertThatThrownBy(() -> ConflictRetry.onConflict(() -> {
            attempts.incrementAndGet();
            throw conflict();
        })).isInstanceOf(OptimisticConcurrencyException.class);

        assertThat(attempts).hasValue(ConflictRetry.MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("given a business rejection, does not retry")
    void rejectionIsNotRetried() {
        var attempts = new AtomicInteger();

        assertThatThrownBy(() -> ConflictRetry.onConflict(() -> {
            attempts.incrementAndGet();
            throw new CommandRejectedException("rule violated");
        })).isInstanceOf(CommandRejectedException.class);

        assertThat(attempts).hasValue(1);
    }

    private static OptimisticConcurrencyException conflict() {
        return new OptimisticConcurrencyException("conflict", null);
    }
}
