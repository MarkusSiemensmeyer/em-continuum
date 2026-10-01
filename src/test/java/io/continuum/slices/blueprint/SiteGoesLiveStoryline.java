package io.continuum.slices.blueprint;

import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.events.ItemReportedToAssetRegistry;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.slices.blueprint.openlocation.OpenLocationCommand;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommand;
import io.continuum.slices.blueprint.translation.facilitysitestatus.SiteStatusChanged;
import io.continuum.slices.blueprint.translation.reportitemtoassetregistry.AssetRegistryNotice;

import java.time.Instant;

/**
 * Storyline "Site goes live", beat by beat, as narrated on the board - every Event Modeling pattern
 * in one flow: an external system's message is translated into our command (inbound translation),
 * an automation reacts with its todo list, and our event is translated into an external system's
 * message (outbound translation). Each slice's {@code funcore/*Specification} replays the beats
 * it owns in a {@code @Nested} class.
 */
public final class SiteGoesLiveStoryline {

    public static final String TITLE = "Site goes live";

    public static final Instant REGISTERED_AT = Instant.parse("2026-10-01T07:00:00Z");
    public static final Instant OPENED_AT = Instant.parse("2026-10-01T08:00:00Z");
    public static final Instant REPORTED_AT = Instant.parse("2026-10-01T08:00:05Z");

    /** 1. COMMAND - a compressor is registered at the Hamburg location, which isn't open yet. */
    public static final RegisterItemCommand REGISTER_ITEM = new RegisterItemCommand("item-9", "Compressor", "loc-hamburg", false);

    /** 2. EVENT */
    public static final ItemRegistered ITEM_REGISTERED = new ItemRegistered("item-9", "Compressor", "loc-hamburg", false, REGISTERED_AT);

    /** 3. EXTERNAL - the facility management system reports the Hamburg site operational. */
    public static final SiteStatusChanged SITE_OPERATIONAL = new SiteStatusChanged("HAMBURG", "OPERATIONAL");

    /** 4. TRANSLATION → COMMAND - FacilitySiteStatus opens the location. */
    public static final OpenLocationCommand OPEN_LOCATION = new OpenLocationCommand("loc-hamburg");

    /** 5. EVENT */
    public static final LocationOpened LOCATION_OPENED = new LocationOpened("loc-hamburg", OPENED_AT);

    /** 6. AUTOMATION → COMMAND - ActivateItemsAtOpenedLocation activates the waiting compressor. */
    public static final ActivateItemCommand ACTIVATE_ITEM = new ActivateItemCommand("item-9");

    /** 7. EVENT */
    public static final ItemActivated ITEM_ACTIVATED = new ItemActivated("item-9", OPENED_AT);

    /** 8. TRANSLATION → EXTERNAL - ReportItemToAssetRegistry tells the asset registry it's in service. */
    public static final AssetRegistryNotice ASSET_IN_SERVICE = new AssetRegistryNotice("item-9", "IN_SERVICE", OPENED_AT);

    /** 9. EVENT - recorded once the registry accepted the notice. */
    public static final ItemReportedToAssetRegistry ITEM_REPORTED = new ItemReportedToAssetRegistry("item-9", REPORTED_AT);

    private SiteGoesLiveStoryline() {
    }
}
