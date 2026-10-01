package io.continuum.slices.blueprint.translation.reportitemtoassetregistry;

import java.time.Instant;

/**
 * Message TO the external asset registry, in ITS vocabulary. {@code assetId} doubles as the
 * idempotency key: the registry must treat a repeated notice for the same asset as a no-op, since
 * this translation delivers at least once.
 */
public record AssetRegistryNotice(String assetId, String lifecycleState, Instant effectiveFrom) {
}
