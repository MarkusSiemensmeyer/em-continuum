package io.continuum.eventstore;

/**
 * Thrown when a {@link DecisionModelLoader#append} call is rejected because an event
 * matching the decision's own query was appended by someone else between the decision's
 * read and its append - i.e. UmaDB's {@code AppendCondition} failed
 * ({@code UmaDbException.IntegrityException}). Callers may retry the whole command from
 * scratch via {@link ConflictRetry}; once retries are exhausted, entry points surface it as a conflict (HTTP 409).
 */
public class OptimisticConcurrencyException extends RuntimeException {
    public OptimisticConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
