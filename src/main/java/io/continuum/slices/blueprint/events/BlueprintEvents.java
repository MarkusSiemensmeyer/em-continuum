package io.continuum.slices.blueprint.events;

import io.continuum.eventstore.EventMapping;

import java.util.List;

/**
 * The {@code blueprint} context's one {@link EventMapping}: type string, tags and payload class
 * per event, used by every imperative shell in this context to fold and append. Kept in one
 * place so an event is tagged the same way no matter which slice appends it.
 */
public final class BlueprintEvents implements EventMapping<BlueprintEvent> {

    public static final BlueprintEvents MAPPING = new BlueprintEvents();

    private BlueprintEvents() {
    }

    @Override
    public String typeOf(BlueprintEvent event) {
        return switch (event) {
            case ItemRegistered e -> ItemRegistered.TYPE;
            case ItemActivated e -> ItemActivated.TYPE;
            case LocationOpened e -> LocationOpened.TYPE;
            case ItemReportedToAssetRegistry e -> ItemReportedToAssetRegistry.TYPE;
        };
    }

    @Override
    public List<String> tagsOf(BlueprintEvent event) {
        return switch (event) {
            case ItemRegistered e -> List.of(
                    EventTags.tag(EventTags.ITEM_ID, e.itemId()),
                    EventTags.tag(EventTags.LOCATION_ID, e.locationId()));
            case ItemActivated e -> List.of(EventTags.tag(EventTags.ITEM_ID, e.itemId()));
            case LocationOpened e -> List.of(EventTags.tag(EventTags.LOCATION_ID, e.locationId()));
            case ItemReportedToAssetRegistry e -> List.of(EventTags.tag(EventTags.ITEM_ID, e.itemId()));
        };
    }

    @Override
    public Class<? extends BlueprintEvent> classOf(String type) {
        return switch (type) {
            case ItemRegistered.TYPE -> ItemRegistered.class;
            case ItemActivated.TYPE -> ItemActivated.class;
            case LocationOpened.TYPE -> LocationOpened.class;
            case ItemReportedToAssetRegistry.TYPE -> ItemReportedToAssetRegistry.class;
            default -> throw new IllegalArgumentException("Not a blueprint event type: " + type);
        };
    }
}
