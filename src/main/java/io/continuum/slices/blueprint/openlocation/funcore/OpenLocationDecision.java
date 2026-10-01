package io.continuum.slices.blueprint.openlocation.funcore;

import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.slices.blueprint.openlocation.OpenLocationCommand;

import java.time.Instant;
import java.util.List;

/**
 * <b>Functional core</b> of the OpenLocation slice - pure, see {@code RegisterItemDecision} for
 * the shape. Opening an already open location changes nothing, so it decides no events rather
 * than rejecting.
 */
public final class OpenLocationDecision {

    public record State(boolean opened) {
    }

    public static final State INITIAL = new State(false);

    private OpenLocationDecision() {
    }

    public static State evolve(State state, BlueprintEvent event) {
        return switch (event) {
            case LocationOpened e -> new State(true);
            default -> state;
        };
    }

    public static List<BlueprintEvent> decide(State state, OpenLocationCommand command, Instant now) {
        if (state.opened()) {
            return List.of();
        }
        return List.of(new LocationOpened(command.locationId(), now));
    }
}
