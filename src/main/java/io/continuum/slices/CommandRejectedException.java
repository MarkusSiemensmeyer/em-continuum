package io.continuum.slices;

/**
 * Thrown by a slice's functional core ({@code {SliceName}Decision.decide}) when the command
 * violates a business rule, given the facts it was handed. Deliberately not in the
 * {@code eventstore} package: the core throws it and must not depend on infrastructure.
 * <p>
 * Never retried by {@code ConflictRetry} - the same facts would give the same rejection.
 * Entry points map it to a rejection of their own (e.g. HTTP 422).
 */
public class CommandRejectedException extends RuntimeException {
    public CommandRejectedException(String message) {
        super(message);
    }
}
