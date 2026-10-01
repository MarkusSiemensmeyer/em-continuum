package io.continuum.eventstore;

import io.umadb.client.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;

/**
 * The two I/O calls every write slice's <b>imperative shell</b> makes around its functional core -
 * this project's hand-rolled equivalent of what an event-sourcing framework's aggregate repository
 * would normally do, since the raw UmaDB client has no such layer of its own.
 * <p>
 * A command handler, inside {@link ConflictRetry#onConflict}: (1) builds its consistency boundary
 * - a {@link Query} over the facts its decision needs - (2) calls {@link #fold} to fold the
 * matching events, decoded to typed domain events, into its core's immutable state, (3) hands that
 * state to its core's pure {@code decide}, and (4) calls {@link #append} with the decided events,
 * the SAME query, and the position {@link #fold} observed. A conflicting event appended
 * concurrently fails the append instead of silently corrupting the decision - UmaDB's
 * {@code AppendCondition.failIfExistsAfter} is what makes this a real consistency boundary, not
 * just a convention.
 */
@Component
public class DecisionModelLoader {

    private final UmaDbClient client;

    public DecisionModelLoader(UmaDbClient client) {
        this.client = client;
    }

    /** The state a functional core decides on, plus the position observed while folding it. */
    public record Folded<S>(S state, long lastPosition) {
    }

    /**
     * Reads every event matching {@code query}, decodes each into the context's typed domain event
     * via {@code mapping}, and folds them into an immutable state with the core's own pure
     * {@code evolve} function - so the core never sees an {@code io.umadb.client} type.
     */
    public <E, S> Folded<S> fold(Query query, EventMapping<E> mapping, S initial, BiFunction<S, ? super E, S> evolve) {
        long lastPosition = client.getHeadPosition();
        S state = initial;
        var batches = client.handle(ReadRequest.of(query));
        while (batches.hasNext()) {
            for (SequencedEvent sequencedEvent : batches.next().events()) {
                state = evolve.apply(state, mapping.decode(sequencedEvent.event()));
            }
        }
        return new Folded<>(state, lastPosition);
    }

    /**
     * Appends all events a functional core decided on in ONE request - all or nothing - guarded by
     * an {@code AppendCondition} that fails if any event matching {@code consistencyBoundary} was
     * appended after {@code lastPosition}, i.e. since {@link #fold} read the state the core decided
     * on. An empty list is a no-op: the core decided nothing needs to happen.
     *
     * @param consistencyBoundary same query {@link #fold} used to read the state this append follows from
     * @param lastPosition        the position {@link #fold} returned alongside that state
     * @throws OptimisticConcurrencyException if a conflicting event was appended concurrently
     */
    public <E> void append(List<? extends E> events, EventMapping<E> mapping, Query consistencyBoundary, long lastPosition) {
        if (events.isEmpty()) {
            return;
        }
        List<Event> encoded = events.stream().map(mapping::encode).toList();
        try {
            client.handle(new AppendRequest(
                    encoded,
                    AppendCondition.failIfExistsAfter(consistencyBoundary, lastPosition)
            ));
        } catch (UmaDbException.IntegrityException e) {
            throw new OptimisticConcurrencyException(
                    "Conflicting event(s) matching the consistency boundary were appended concurrently for event types "
                            + encoded.stream().map(Event::type).toList(), e);
        }
    }
}
