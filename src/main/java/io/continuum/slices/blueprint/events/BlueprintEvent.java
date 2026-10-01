package io.continuum.slices.blueprint.events;

/**
 * Every event of the {@code blueprint} context. Sealed, so the switches in
 * {@link BlueprintEvents} and in each slice's functional core are checked for exhaustiveness
 * by the compiler when an event is added.
 */
public sealed interface BlueprintEvent permits ItemRegistered, ItemActivated, LocationOpened, ItemReportedToAssetRegistry {
}
