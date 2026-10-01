package io.continuum.slices.blueprint.openlocation.funcore;

import io.continuum.slices.blueprint.LocationOpeningStoryline;
import io.continuum.slices.blueprint.SiteGoesLiveStoryline;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.slices.blueprint.openlocation.OpenLocationCommand;
import io.continuum.testsupport.spec.DecisionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/** OpenLocation - the board's specifications, one test each, run against the pure core. */
class OpenLocationSpecification {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private static final DecisionSpecification<OpenLocationDecision.State, OpenLocationCommand, BlueprintEvent> SPEC =
            DecisionSpecification.of(OpenLocationDecision.INITIAL, OpenLocationDecision::evolve,
                    (state, command) -> OpenLocationDecision.decide(state, command, NOW));

    @Test
    @DisplayName("given nothing, when OpenLocation, then LocationOpened")
    void opensLocation() {
        SPEC.given()
                .when(new OpenLocationCommand("loc-1"))
                .then(new LocationOpened("loc-1", NOW));
    }

    @Test
    @DisplayName("given LocationOpened, when OpenLocation, then nothing")
    void ignoresOpenLocation() {
        SPEC.given(new LocationOpened("loc-1", NOW))
                .when(new OpenLocationCommand("loc-1"))
                .thenNothing();
    }

    @Nested
    @DisplayName("Storyline: " + LocationOpeningStoryline.TITLE)
    class LocationOpening {

        private static final DecisionSpecification<OpenLocationDecision.State, OpenLocationCommand, BlueprintEvent> SPEC =
                DecisionSpecification.of(OpenLocationDecision.INITIAL, OpenLocationDecision::evolve,
                        (state, command) -> OpenLocationDecision.decide(state, command, LocationOpeningStoryline.OPENED_AT));

        @Test
        @DisplayName("4 → 5: OpenLocation results in LocationOpened")
        void openLocation() {
            SPEC.given()
                    .when(LocationOpeningStoryline.OPEN_LOCATION)
                    .then(LocationOpeningStoryline.LOCATION_OPENED);
        }
    }

    @Nested
    @DisplayName("Storyline: " + SiteGoesLiveStoryline.TITLE)
    class SiteGoesLive {

        private static final DecisionSpecification<OpenLocationDecision.State, OpenLocationCommand, BlueprintEvent> SPEC =
                DecisionSpecification.of(OpenLocationDecision.INITIAL, OpenLocationDecision::evolve,
                        (state, command) -> OpenLocationDecision.decide(state, command, SiteGoesLiveStoryline.OPENED_AT));

        @Test
        @DisplayName("4 → 5: OpenLocation results in LocationOpened")
        void openLocation() {
            SPEC.given()
                    .when(SiteGoesLiveStoryline.OPEN_LOCATION)
                    .then(SiteGoesLiveStoryline.LOCATION_OPENED);
        }
    }
}
