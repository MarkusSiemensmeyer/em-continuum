package io.continuum.slices.blueprint.items.funcore;

import java.time.Instant;

/** One row of the Items read model - what the functional core computes and the query returns. */
public record ItemSummary(String itemId, String name, ItemStatus status, Instant registeredAt, Instant activatedAt) {
}
