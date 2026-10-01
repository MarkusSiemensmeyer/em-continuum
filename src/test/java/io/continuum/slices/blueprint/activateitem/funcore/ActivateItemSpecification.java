package io.continuum.slices.blueprint.activateitem.funcore;

import io.continuum.slices.blueprint.ItemLifecycleStoryline;
import io.continuum.slices.blueprint.LocationOpeningStoryline;
import io.continuum.slices.blueprint.SiteGoesLiveStoryline;
import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.testsupport.spec.DecisionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/** ActivateItem - the board's specifications, one test each, run against the pure core. */
class ActivateItemSpecification {

    private static final Instant REGISTERED = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-01T10:05:00Z");

    private static final DecisionSpecification<ActivateItemDecision.State, ActivateItemCommand, BlueprintEvent> SPEC =
            DecisionSpecification.of(ActivateItemDecision.INITIAL, ActivateItemDecision::evolve,
                    (state, command) -> ActivateItemDecision.decide(state, command, NOW));

    @Test
    @DisplayName("given ItemRegistered, when ActivateItem, then ItemActivated")
    void activatesRegisteredItem() {
        SPEC.given(new ItemRegistered("item-1", "Pump", "loc-1", true, REGISTERED))
                .when(new ActivateItemCommand("item-1"))
                .then(new ItemActivated("item-1", NOW));
    }

    @Test
    @DisplayName("given nothing, when ActivateItem, then rejected")
    void rejectsUnknownItem() {
        SPEC.given()
                .when(new ActivateItemCommand("item-1"))
                .thenRejected();
    }

    @Test
    @DisplayName("given ItemRegistered and ItemActivated, when ActivateItem, then nothing")
    void ignoresRepeatedActivation() {
        SPEC.given(new ItemRegistered("item-1", "Pump", "loc-1", true, REGISTERED), new ItemActivated("item-1", REGISTERED))
                .when(new ActivateItemCommand("item-1"))
                .thenNothing();
    }

    @Nested
    @DisplayName("Storyline: " + ItemLifecycleStoryline.TITLE)
    class ItemLifecycle {

        private static final DecisionSpecification<ActivateItemDecision.State, ActivateItemCommand, BlueprintEvent> SPEC =
                DecisionSpecification.of(ActivateItemDecision.INITIAL, ActivateItemDecision::evolve,
                        (state, command) -> ActivateItemDecision.decide(state, command, ItemLifecycleStoryline.ACTIVATED_AT));

        @Test
        @DisplayName("2 → 4 → 5: after ItemRegistered, ActivateItem results in ItemActivated")
        void activateItem() {
            SPEC.given(ItemLifecycleStoryline.ITEM_REGISTERED)
                    .when(ItemLifecycleStoryline.ACTIVATE_ITEM)
                    .then(ItemLifecycleStoryline.ITEM_ACTIVATED);
        }
    }

    @Nested
    @DisplayName("Storyline: " + LocationOpeningStoryline.TITLE)
    class LocationOpening {

        private static final DecisionSpecification<ActivateItemDecision.State, ActivateItemCommand, BlueprintEvent> SPEC =
                DecisionSpecification.of(ActivateItemDecision.INITIAL, ActivateItemDecision::evolve,
                        (state, command) -> ActivateItemDecision.decide(state, command, LocationOpeningStoryline.OPENED_AT));

        @Test
        @DisplayName("2, 5 → 6 → 7: after ItemRegistered and LocationOpened, ActivateItem results in ItemActivated")
        void activateItem() {
            SPEC.given(LocationOpeningStoryline.ITEM_REGISTERED, LocationOpeningStoryline.LOCATION_OPENED)
                    .when(LocationOpeningStoryline.ACTIVATE_ITEM)
                    .then(LocationOpeningStoryline.ITEM_ACTIVATED);
        }
    }

    @Nested
    @DisplayName("Storyline: " + SiteGoesLiveStoryline.TITLE)
    class SiteGoesLive {

        private static final DecisionSpecification<ActivateItemDecision.State, ActivateItemCommand, BlueprintEvent> SPEC =
                DecisionSpecification.of(ActivateItemDecision.INITIAL, ActivateItemDecision::evolve,
                        (state, command) -> ActivateItemDecision.decide(state, command, SiteGoesLiveStoryline.OPENED_AT));

        @Test
        @DisplayName("2, 5 → 6 → 7: after ItemRegistered and LocationOpened, ActivateItem results in ItemActivated")
        void activateItem() {
            SPEC.given(SiteGoesLiveStoryline.ITEM_REGISTERED, SiteGoesLiveStoryline.LOCATION_OPENED)
                    .when(SiteGoesLiveStoryline.ACTIVATE_ITEM)
                    .then(SiteGoesLiveStoryline.ITEM_ACTIVATED);
        }
    }
}
