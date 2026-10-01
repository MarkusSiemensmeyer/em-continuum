package io.continuum.slices.blueprint.activateitem.funcore;

import io.continuum.slices.CommandRejectedException;
import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;

import java.time.Instant;
import java.util.List;

/**
 * <b>Functional core</b> of the ActivateItem slice - pure, see {@code RegisterItemDecision} for
 * the shape. Shows the three outcomes a core can have:
 * <ul>
 *   <li>events to append - the item gets activated</li>
 *   <li>{@link CommandRejectedException} - a rule is violated (item unknown)</li>
 *   <li>no events - nothing to do (already activated), so a repeated command, e.g. from the
 *       automation replaying on restart, is a harmless no-op instead of an error</li>
 * </ul>
 */
public final class ActivateItemDecision {

    public record State(boolean registered, boolean activated) {
    }

    public static final State INITIAL = new State(false, false);

    private ActivateItemDecision() {
    }

    public static State evolve(State state, BlueprintEvent event) {
        return switch (event) {
            case ItemRegistered e -> new State(true, state.activated());
            case ItemActivated e -> new State(state.registered(), true);
            default -> state;
        };
    }

    public static List<BlueprintEvent> decide(State state, ActivateItemCommand command, Instant now) {
        if (!state.registered()) {
            throw new CommandRejectedException("Item " + command.itemId() + " is not registered");
        }
        if (state.activated()) {
            return List.of();
        }
        return List.of(new ItemActivated(command.itemId(), now));
    }
}
