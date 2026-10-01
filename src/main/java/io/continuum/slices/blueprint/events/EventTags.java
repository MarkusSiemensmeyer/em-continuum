package io.continuum.slices.blueprint.events;

/** {@code "key:value"} tag strings the {@code blueprint} context's events are appended and queried with. */
public final class EventTags {

    public static final String ITEM_ID = "itemId";
    public static final String LOCATION_ID = "locationId";

    private EventTags() {
    }

    public static String tag(String key, String value) {
        return key + ":" + value;
    }
}
