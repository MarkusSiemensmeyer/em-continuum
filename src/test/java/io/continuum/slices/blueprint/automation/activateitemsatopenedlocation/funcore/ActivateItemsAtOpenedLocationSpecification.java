package io.continuum.slices.blueprint.automation.activateitemsatopenedlocation.funcore;

import io.continuum.slices.blueprint.LocationOpeningStoryline;
import io.continuum.slices.blueprint.SiteGoesLiveStoryline;
import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.automation.activateitemsatopenedlocation.funcore.ActivateItemsAtOpenedLocationPolicy.TodoList;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.testsupport.spec.ReadModelAutomationSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * ActivateItemsAtOpenedLocation - the board's specifications, one test each, run against the pure
 * policy. As on the board: setup events first, the trigger last.
 */
class ActivateItemsAtOpenedLocationSpecification {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private static final ReadModelAutomationSpecification<TodoList, BlueprintEvent, ActivateItemCommand> SPEC =
            ReadModelAutomationSpecification.of(TodoList.EMPTY,
                    ActivateItemsAtOpenedLocationPolicy::evolve, ActivateItemsAtOpenedLocationPolicy::react);

    @Test
    @DisplayName("given items registered at several locations, when one location opens, then only its items are activated")
    void activatesItemsAtOpenedLocationOnly() {
        SPEC.given(
                        new ItemRegistered("item-2", "Valve", "loc-1", false, NOW),
                        new ItemRegistered("item-3", "Filter", "loc-2", false, NOW),
                        new ItemRegistered("item-1", "Pump", "loc-1", false, NOW),
                        new LocationOpened("loc-1", NOW))
                .then(new ActivateItemCommand("item-1"), new ActivateItemCommand("item-2"));
    }

    @Test
    @DisplayName("given no item registered at the location, when it opens, then nothing")
    void nothingPendingAtLocation() {
        SPEC.given(
                        new ItemRegistered("item-1", "Pump", "loc-2", false, NOW),
                        new LocationOpened("loc-1", NOW))
                .thenNothing();
    }

    @Test
    @DisplayName("given an item already activated, when its location opens, then it is not activated again")
    void activatedItemIsDone() {
        SPEC.given(
                        new ItemRegistered("item-1", "Pump", "loc-1", true, NOW),
                        new ItemActivated("item-1", NOW),
                        new LocationOpened("loc-1", NOW))
                .thenNothing();
    }

    @Test
    @DisplayName("given the location opened before the item was registered, then nothing")
    void setupAfterTriggerDoesNotCount() {
        SPEC.given(
                        new LocationOpened("loc-1", NOW),
                        new ItemRegistered("item-1", "Pump", "loc-1", false, NOW))
                .thenNothing();
    }

    @Nested
    @DisplayName("Storyline: " + LocationOpeningStoryline.TITLE)
    class LocationOpening {

        @Test
        @DisplayName("2, 5 → 6: after ItemRegistered, LocationOpened triggers ActivateItem")
        void activateItemsAtOpenedLocation() {
            SPEC.given(LocationOpeningStoryline.ITEM_REGISTERED, LocationOpeningStoryline.LOCATION_OPENED)
                    .then(LocationOpeningStoryline.ACTIVATE_ITEM);
        }
    }

    @Nested
    @DisplayName("Storyline: " + SiteGoesLiveStoryline.TITLE)
    class SiteGoesLive {

        @Test
        @DisplayName("2, 5 → 6: after ItemRegistered, LocationOpened triggers ActivateItem")
        void activateItemsAtOpenedLocation() {
            SPEC.given(SiteGoesLiveStoryline.ITEM_REGISTERED, SiteGoesLiveStoryline.LOCATION_OPENED)
                    .then(SiteGoesLiveStoryline.ACTIVATE_ITEM);
        }
    }
}
