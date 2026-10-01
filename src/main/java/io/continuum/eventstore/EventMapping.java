package io.continuum.eventstore;

import io.umadb.client.Event;

import java.util.List;

/**
 * Translates one context's typed domain events to and from UmaDB's raw {@link Event} - the
 * persistence knowledge (type string, tags, payload class) the imperative shell needs so that
 * the functional core only ever sees typed domain events, never {@code io.umadb.client} types.
 * <p>
 * Implemented once per context (e.g. {@code BlueprintEvents}), next to that context's sealed
 * event interface, so an event gets the same type and tags no matter which slice appends it.
 * Tags decide which queries can find an event later, so an event should be tagged with every id
 * it carries.
 */
public interface EventMapping<E> {

    /** UmaDB {@code Event.type()} for this domain event. */
    String typeOf(E event);

    /** Tags ({@code "key:value"}) this domain event is appended with. */
    List<String> tagsOf(E event);

    /** Domain event record class stored under the given UmaDB type. */
    Class<? extends E> classOf(String type);

    default Event encode(E event) {
        return EventCodec.toEvent(event, typeOf(event), tagsOf(event));
    }

    default E decode(Event event) {
        return EventCodec.fromEvent(event, classOf(event.type()));
    }
}
