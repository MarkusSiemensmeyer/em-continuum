package io.continuum.slices.blueprint.events;

import java.time.Instant;

public record ItemActivated(String itemId, Instant activatedAt) implements BlueprintEvent {

    public static final String TYPE = "Blueprint.ItemActivated";
}
