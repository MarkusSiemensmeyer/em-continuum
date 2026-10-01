package io.continuum.slices.blueprint.registeritem.funcore;

import io.continuum.slices.blueprint.ItemLifecycleStoryline;
import io.continuum.slices.blueprint.LocationOpeningStoryline;
import io.continuum.slices.blueprint.SiteGoesLiveStoryline;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommand;
import io.continuum.testsupport.spec.DecisionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/** RegisterItem - the board's specifications, one test each, run against the pure core. */
class RegisterItemSpecification {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private static final DecisionSpecification<RegisterItemDecision.State, RegisterItemCommand, BlueprintEvent> SPEC =
            DecisionSpecification.of(RegisterItemDecision.INITIAL, RegisterItemDecision::evolve,
                    (state, command) -> RegisterItemDecision.decide(state, command, NOW));

    @Test
    @DisplayName("given nothing, when RegisterItem, then ItemRegistered")
    void registersNewItem() {
        SPEC.given()
                .when(new RegisterItemCommand("item-1", "Pump", "loc-1", true))
                .then(new ItemRegistered("item-1", "Pump", "loc-1", true, NOW));
    }

    @Test
    @DisplayName("given ItemRegistered, when RegisterItem for the same item, then rejected")
    void rejectsDuplicateRegistration() {
        SPEC.given(new ItemRegistered("item-1", "Pump", "loc-1", false, NOW))
                .when(new RegisterItemCommand("item-1", "Valve", "loc-1", false))
                .thenRejected();
    }

    @Nested
    @DisplayName("Storyline: " + ItemLifecycleStoryline.TITLE)
    class ItemLifecycle {

        private static final DecisionSpecification<RegisterItemDecision.State, RegisterItemCommand, BlueprintEvent> SPEC =
                DecisionSpecification.of(RegisterItemDecision.INITIAL, RegisterItemDecision::evolve,
                        (state, command) -> RegisterItemDecision.decide(state, command, ItemLifecycleStoryline.REGISTERED_AT));

        @Test
        @DisplayName("1 → 2: RegisterItem results in ItemRegistered")
        void registerItem() {
            SPEC.given()
                    .when(ItemLifecycleStoryline.REGISTER_ITEM)
                    .then(ItemLifecycleStoryline.ITEM_REGISTERED);
        }
    }

    @Nested
    @DisplayName("Storyline: " + LocationOpeningStoryline.TITLE)
    class LocationOpening {

        private static final DecisionSpecification<RegisterItemDecision.State, RegisterItemCommand, BlueprintEvent> SPEC =
                DecisionSpecification.of(RegisterItemDecision.INITIAL, RegisterItemDecision::evolve,
                        (state, command) -> RegisterItemDecision.decide(state, command, LocationOpeningStoryline.REGISTERED_AT));

        @Test
        @DisplayName("1 → 2: RegisterItem results in ItemRegistered")
        void registerItem() {
            SPEC.given()
                    .when(LocationOpeningStoryline.REGISTER_ITEM)
                    .then(LocationOpeningStoryline.ITEM_REGISTERED);
        }
    }

    @Nested
    @DisplayName("Storyline: " + SiteGoesLiveStoryline.TITLE)
    class SiteGoesLive {

        private static final DecisionSpecification<RegisterItemDecision.State, RegisterItemCommand, BlueprintEvent> SPEC =
                DecisionSpecification.of(RegisterItemDecision.INITIAL, RegisterItemDecision::evolve,
                        (state, command) -> RegisterItemDecision.decide(state, command, SiteGoesLiveStoryline.REGISTERED_AT));

        @Test
        @DisplayName("1 → 2: RegisterItem results in ItemRegistered")
        void registerItem() {
            SPEC.given()
                    .when(SiteGoesLiveStoryline.REGISTER_ITEM)
                    .then(SiteGoesLiveStoryline.ITEM_REGISTERED);
        }
    }
}
