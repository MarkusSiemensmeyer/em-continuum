package io.continuum.slices.blueprint.events;

import java.time.Instant;

public record LocationOpened(String locationId, Instant openedAt) implements BlueprintEvent {

    public static final String TYPE = "Blueprint.LocationOpened";
}
