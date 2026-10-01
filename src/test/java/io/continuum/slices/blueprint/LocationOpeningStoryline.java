package io.continuum.slices.blueprint;

import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.slices.blueprint.items.funcore.ItemStatus;
import io.continuum.slices.blueprint.items.funcore.ItemSummary;
import io.continuum.slices.blueprint.openlocation.OpenLocationCommand;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommand;

import java.time.Instant;

/**
 * Storyline "Location opening", beat by beat, as narrated on the board: an item registered at a
 * location that isn't open yet waits on the ActivateItemsAtOpenedLocation automation's todo list
 * until the location opens. Each slice's {@code funcore/*Specification} replays the beats it owns
 * in a {@code @Nested} class.
 */
public final class LocationOpeningStoryline {

    public static final String TITLE = "Location opening";

    public static final Instant REGISTERED_AT = Instant.parse("2026-10-01T08:00:00Z");
    public static final Instant OPENED_AT = Instant.parse("2026-10-01T09:00:00Z");

    /** 1. COMMAND - a valve is registered at the Berlin location, not for immediate activation. */
    public static final RegisterItemCommand REGISTER_ITEM = new RegisterItemCommand("item-7", "Valve", "loc-berlin", false);

    /** 2. EVENT */
    public static final ItemRegistered ITEM_REGISTERED = new ItemRegistered("item-7", "Valve", "loc-berlin", false, REGISTERED_AT);

    /** 3. READMODEL - Items shows the valve as registered. */
    public static final ItemSummary ITEMS_SHOW_REGISTERED =
            new ItemSummary("item-7", "Valve", ItemStatus.REGISTERED, REGISTERED_AT, null);

    /** 4. COMMAND - the Berlin location opens. */
    public static final OpenLocationCommand OPEN_LOCATION = new OpenLocationCommand("loc-berlin");

    /** 5. EVENT */
    public static final LocationOpened LOCATION_OPENED = new LocationOpened("loc-berlin", OPENED_AT);

    /** 6. AUTOMATION → COMMAND - ActivateItemsAtOpenedLocation activates the waiting valve. */
    public static final ActivateItemCommand ACTIVATE_ITEM = new ActivateItemCommand("item-7");

    /** 7. EVENT */
    public static final ItemActivated ITEM_ACTIVATED = new ItemActivated("item-7", OPENED_AT);

    /** 8. READMODEL - Items shows the valve as active. */
    public static final ItemSummary ITEMS_SHOW_ACTIVE =
            new ItemSummary("item-7", "Valve", ItemStatus.ACTIVE, REGISTERED_AT, OPENED_AT);

    private LocationOpeningStoryline() {
    }
}
