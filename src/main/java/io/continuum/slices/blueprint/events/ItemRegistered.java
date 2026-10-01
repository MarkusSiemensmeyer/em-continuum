package io.continuum.slices.blueprint.events;

import java.time.Instant;

public record ItemRegistered(String itemId, String name, String locationId, boolean activateImmediately, Instant registeredAt)
        implements BlueprintEvent {

    public static final String TYPE = "Blueprint.ItemRegistered";
}
