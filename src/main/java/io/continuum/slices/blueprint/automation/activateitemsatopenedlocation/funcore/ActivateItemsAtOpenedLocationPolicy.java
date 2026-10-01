package io.continuum.slices.blueprint.automation.activateitemsatopenedlocation.funcore;

import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.events.LocationOpened;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Functional core</b> of the ActivateItemsAtOpenedLocation automation - an automation with a
 * private read model (Event Modeling's "todo list"): items registered at a location get activated
 * once that location opens.
 * <ul>
 *   <li>{@link #evolve} - setup events change the {@link TodoList}: registered items are added,
 *       activated items are done</li>
 *   <li>{@link #react} - the trigger ({@link LocationOpened}) turns every pending item at that
 *       location into an {@link ActivateItemCommand}</li>
 * </ul>
 * Both are pure; the shell ({@code ActivateItemsAtOpenedLocationProcessor}) holds the current todo
 * list and dispatches the commands.
 */
public final class ActivateItemsAtOpenedLocationPolicy {

    /** The private read model: items not activated yet, item id → location id. Immutable. */
    public record TodoList(Map<String, String> pendingItemLocations) {

        public static final TodoList EMPTY = new TodoList(Map.of());

        public TodoList {
            pendingItemLocations = Map.copyOf(pendingItemLocations);
        }

        TodoList with(String itemId, String locationId) {
            var pending = new HashMap<>(pendingItemLocations);
            pending.put(itemId, locationId);
            return new TodoList(pending);
        }

        TodoList without(String itemId) {
            var pending = new HashMap<>(pendingItemLocations);
            pending.remove(itemId);
            return new TodoList(pending);
        }
    }

    private ActivateItemsAtOpenedLocationPolicy() {
    }

    public static TodoList evolve(TodoList todoList, BlueprintEvent event) {
        return switch (event) {
            case ItemRegistered e -> todoList.with(e.itemId(), e.locationId());
            case ItemActivated e -> todoList.without(e.itemId());
            default -> todoList;
        };
    }

    public static List<ActivateItemCommand> react(TodoList todoList, BlueprintEvent event) {
        return switch (event) {
            case LocationOpened e -> todoList.pendingItemLocations().entrySet().stream()
                    .filter(pending -> pending.getValue().equals(e.locationId()))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .map(ActivateItemCommand::new)
                    .toList();
            default -> List.of();
        };
    }
}
