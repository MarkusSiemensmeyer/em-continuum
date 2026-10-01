package io.continuum.slices.blueprint.items.funcore;

import io.continuum.slices.blueprint.ItemLifecycleStoryline;
import io.continuum.slices.blueprint.LocationOpeningStoryline;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.testsupport.spec.ProjectionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/** Items - the board's specifications, one test each, run against the pure projection. */
class ItemsSpecification {

    private static final Instant REGISTERED = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant ACTIVATED = Instant.parse("2026-10-01T10:05:00Z");

    private static final ProjectionSpecification<ItemSummary, BlueprintEvent> SPEC =
            ProjectionSpecification.of(ItemsProjection::evolve);

    @Test
    @DisplayName("given nothing, then no item")
    void empty() {
        SPEC.given()
                .thenNothing();
    }

    @Test
    @DisplayName("given ItemRegistered, then item shown as REGISTERED")
    void registered() {
        SPEC.given(new ItemRegistered("item-1", "Pump", "loc-1", false, REGISTERED))
                .then(new ItemSummary("item-1", "Pump", ItemStatus.REGISTERED, REGISTERED, null));
    }

    @Test
    @DisplayName("given ItemRegistered and ItemActivated, then item shown as ACTIVE")
    void activated() {
        SPEC.given(new ItemRegistered("item-1", "Pump", "loc-1", true, REGISTERED), new ItemActivated("item-1", ACTIVATED))
                .then(new ItemSummary("item-1", "Pump", ItemStatus.ACTIVE, REGISTERED, ACTIVATED));
    }

    @Test
    @DisplayName("given ItemRegistered and ItemActivated replayed, then item still shown as ACTIVE")
    void replay() {
        var registered = new ItemRegistered("item-1", "Pump", "loc-1", true, REGISTERED);
        var activated = new ItemActivated("item-1", ACTIVATED);

        SPEC.given(registered, activated, registered, activated)
                .then(new ItemSummary("item-1", "Pump", ItemStatus.ACTIVE, REGISTERED, ACTIVATED));
    }

    @Nested
    @DisplayName("Storyline: " + ItemLifecycleStoryline.TITLE)
    class ItemLifecycle {

        @Test
        @DisplayName("2 → 3: after ItemRegistered, Items shows it as REGISTERED")
        void showsRegistered() {
            SPEC.given(ItemLifecycleStoryline.ITEM_REGISTERED)
                    .then(ItemLifecycleStoryline.ITEMS_SHOW_REGISTERED);
        }

        @Test
        @DisplayName("3 → 5 → 6: after ItemActivated, Items shows it as ACTIVE")
        void showsActive() {
            SPEC.given(ItemLifecycleStoryline.ITEM_REGISTERED, ItemLifecycleStoryline.ITEM_ACTIVATED)
                    .then(ItemLifecycleStoryline.ITEMS_SHOW_ACTIVE);
        }
    }

    @Nested
    @DisplayName("Storyline: " + LocationOpeningStoryline.TITLE)
    class LocationOpening {

        @Test
        @DisplayName("2 → 3: after ItemRegistered, Items shows it as REGISTERED")
        void showsRegistered() {
            SPEC.given(LocationOpeningStoryline.ITEM_REGISTERED)
                    .then(LocationOpeningStoryline.ITEMS_SHOW_REGISTERED);
        }

        @Test
        @DisplayName("3 → 5, 7 → 8: after LocationOpened and ItemActivated, Items shows it as ACTIVE")
        void showsActive() {
            SPEC.given(LocationOpeningStoryline.ITEM_REGISTERED, LocationOpeningStoryline.LOCATION_OPENED,
                            LocationOpeningStoryline.ITEM_ACTIVATED)
                    .then(LocationOpeningStoryline.ITEMS_SHOW_ACTIVE);
        }
    }
}
