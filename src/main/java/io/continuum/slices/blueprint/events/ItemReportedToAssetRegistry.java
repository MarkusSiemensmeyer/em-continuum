package io.continuum.slices.blueprint.events;

import java.time.Instant;

/** Recorded after the asset registry accepted an item's report - so a replay doesn't report it again. */
public record ItemReportedToAssetRegistry(String itemId, Instant reportedAt) implements BlueprintEvent {

    public static final String TYPE = "Blueprint.ItemReportedToAssetRegistry";
}
