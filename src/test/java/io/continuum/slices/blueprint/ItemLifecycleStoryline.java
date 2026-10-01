package io.continuum.slices.blueprint;

import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.items.funcore.ItemStatus;
import io.continuum.slices.blueprint.items.funcore.ItemSummary;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommand;

import java.time.Instant;

/**
 * Storyline "Item lifecycle", beat by beat, as narrated on the board. It crosses four slices, so
 * its beats live here once, and each slice's {@code funcore/*Specification} replays the beats it
 * owns in a {@code @Nested} "Storyline: Item lifecycle" class.
 */
public final class ItemLifecycleStoryline {

    public static final String TITLE = "Item lifecycle";

    public static final Instant REGISTERED_AT = Instant.parse("2026-10-01T09:00:00Z");
    public static final Instant ACTIVATED_AT = Instant.parse("2026-10-01T09:00:01Z");

    /** 1. COMMAND - someone registers a pump for immediate activation. */
    public static final RegisterItemCommand REGISTER_ITEM = new RegisterItemCommand("item-42", "Pump", "loc-1", true);

    /** 2. EVENT */
    public static final ItemRegistered ITEM_REGISTERED = new ItemRegistered("item-42", "Pump", "loc-1", true, REGISTERED_AT);

    /** 3. READMODEL - Items shows the pump as registered. */
    public static final ItemSummary ITEMS_SHOW_REGISTERED =
            new ItemSummary("item-42", "Pump", ItemStatus.REGISTERED, REGISTERED_AT, null);

    /** 4. AUTOMATION → COMMAND - ActivateRegisteredItem activates it. */
    public static final ActivateItemCommand ACTIVATE_ITEM = new ActivateItemCommand("item-42");

    /** 5. EVENT */
    public static final ItemActivated ITEM_ACTIVATED = new ItemActivated("item-42", ACTIVATED_AT);

    /** 6. READMODEL - Items shows the pump as active. */
    public static final ItemSummary ITEMS_SHOW_ACTIVE =
            new ItemSummary("item-42", "Pump", ItemStatus.ACTIVE, REGISTERED_AT, ACTIVATED_AT);

    private ItemLifecycleStoryline() {
    }
}
