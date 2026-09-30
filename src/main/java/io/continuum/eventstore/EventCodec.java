package io.continuum.eventstore;

import io.umadb.client.Event;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Serializes domain event/command records to and from the opaque {@code byte[]} payload
 * an UmaDB {@link Event} carries. UmaDB itself has no notion of a payload schema - this
 * codec is this project's own convention (plain JSON via Jackson 3), not part of the UmaDB
 * client API.
 */
public final class EventCodec {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private EventCodec() {
    }

    /** Serializes a domain event record into a new UmaDB {@link Event} with a generated id. */
    public static Event toEvent(Object domainEvent, String type, List<String> tags) {
        try {
            byte[] data = MAPPER.writeValueAsBytes(domainEvent);
            return Event.of(type, tags, data);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize event of type " + type, e);
        }
    }

    /** Deserializes an UmaDB {@link Event}'s payload back into the given domain event record type. */
    public static <T> T fromEvent(Event event, Class<T> type) {
        try {
            return MAPPER.readValue(event.data(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize event of type " + event.type(), e);
        }
    }
}
