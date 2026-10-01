package io.continuum.slices.blueprint.registeritem.funcore;

import io.continuum.slices.CommandRejectedException;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommand;

import java.time.Instant;
import java.util.List;

/**
 * <b>Functional core</b> of the RegisterItem slice: pure functions over plain values - no I/O,
 * no Spring, no {@code io.umadb.client} types. Everything it needs is handed in by the imperative
 * shell ({@code RegisterItemCommandHandler}).
 * <ul>
 *   <li>{@link #evolve} folds past events into the {@link State} this decision needs - and only that</li>
 *   <li>{@link #decide} turns state + command (+ the current time) into the events to append</li>
 * </ul>
 */
public final class RegisterItemDecision {

    /** The facts this decision branches on: has this item been registered before? */
    public record State(boolean registered) {
    }

    public static final State INITIAL = new State(false);

    private RegisterItemDecision() {
    }

    public static State evolve(State state, BlueprintEvent event) {
        return switch (event) {
            case ItemRegistered e -> new State(true);
            default -> state;
        };
    }

    public static List<BlueprintEvent> decide(State state, RegisterItemCommand command, Instant now) {
        if (state.registered()) {
            throw new CommandRejectedException("Item " + command.itemId() + " is already registered");
        }
        return List.of(new ItemRegistered(command.itemId(), command.name(), command.locationId(), command.activateImmediately(), now));
    }
}
