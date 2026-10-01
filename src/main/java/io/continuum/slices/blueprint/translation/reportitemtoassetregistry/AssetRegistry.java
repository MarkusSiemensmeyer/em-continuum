package io.continuum.slices.blueprint.translation.reportitemtoassetregistry;

/**
 * Port to the external asset registry - the I/O the outbound translation's shell performs.
 * Production: {@link RestClientAssetRegistry}; tests: an in-memory fake.
 */
public interface AssetRegistry {

    /** Delivers the notice; throws if the registry did not accept it. */
    void report(AssetRegistryNotice notice);
}
