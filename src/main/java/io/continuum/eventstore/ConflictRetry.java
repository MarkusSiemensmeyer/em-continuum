package io.continuum.eventstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The retry every write slice's imperative shell wraps itself in - deliberately a plain static
 * call ({@code ConflictRetry.onConflict(() -> ...)}) instead of Spring's {@code @Retryable}, so
 * the retry is visible right in the command handler's code and can't be silently skipped by a
 * self-invocation that bypasses a proxy.
 * <p>
 * Retries <b>only</b> {@link OptimisticConcurrencyException}, immediately (no backoff), up to
 * {@link #MAX_ATTEMPTS} times, and always re-runs the WHOLE attempt: re-read, re-decide,
 * re-append. Retrying just the append with the old position would fail every time, because the
 * conflicting event is still there. A {@code CommandRejectedException} from the functional core
 * is never retried - re-deciding against the same facts gives the same answer.
 * <p>
 * Everything inside the attempt runs again on each retry, so the shell may only contain I/O that
 * is safe to repeat (reads, the conditional append itself). Side effects like sending a mail
 * belong in an automation reacting to the appended event, not in a write slice's shell.
 */
public final class ConflictRetry {

    public static final int MAX_ATTEMPTS = 5;

    private static final Logger log = LoggerFactory.getLogger(ConflictRetry.class);

    private ConflictRetry() {
    }

    /**
     * Runs {@code attempt}, re-running it immediately on {@link OptimisticConcurrencyException}.
     *
     * @throws OptimisticConcurrencyException the last conflict, once {@link #MAX_ATTEMPTS} attempts all conflicted
     */
    public static void onConflict(Runnable attempt) {
        for (int n = 1; ; n++) {
            try {
                attempt.run();
                return;
            } catch (OptimisticConcurrencyException e) {
                if (n >= MAX_ATTEMPTS) {
                    throw e;
                }
                log.debug("Concurrent append conflict on attempt {}/{} - re-reading and re-deciding", n, MAX_ATTEMPTS);
            }
        }
    }
}
