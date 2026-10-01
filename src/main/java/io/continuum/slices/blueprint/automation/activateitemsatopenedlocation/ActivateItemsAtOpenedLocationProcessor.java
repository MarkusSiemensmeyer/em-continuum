package io.continuum.slices.blueprint.automation.activateitemsatopenedlocation;

import io.continuum.eventstore.SliceEventListener;
import io.continuum.slices.blueprint.activateitem.ActivateItemCommandHandler;
import io.continuum.slices.blueprint.automation.activateitemsatopenedlocation.funcore.ActivateItemsAtOpenedLocationPolicy;
import io.continuum.slices.blueprint.automation.activateitemsatopenedlocation.funcore.ActivateItemsAtOpenedLocationPolicy.TodoList;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.umadb.client.Event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * <b>Imperative shell</b> of the ActivateItemsAtOpenedLocation automation. Holds the current
 * {@link TodoList} - the one piece of state the pure policy works on - and does the I/O: decoding
 * raw events and dispatching the resulting commands into ActivateItem's imperative shell.
 * <p>
 * The todo list lives in memory only: {@code EventDispatcher} replays from position 0 on every
 * start, which rebuilds it. Commands re-dispatched during that replay are harmless - ActivateItem's
 * core turns an already activated item into a no-op. {@code EventDispatcher} calls
 * {@link #onEvent} from one thread, in store order.
 */
@Component
@ConditionalOnProperty(prefix = "slices.blueprint.automation", name = "activateitemsatopenedlocation.enabled")
public class ActivateItemsAtOpenedLocationProcessor implements SliceEventListener {

    private final ActivateItemCommandHandler activateItem;

    private volatile TodoList todoList = TodoList.EMPTY;

    public ActivateItemsAtOpenedLocationProcessor(ActivateItemCommandHandler activateItem) {
        this.activateItem = activateItem;
    }

    @Override
    public boolean supports(String eventType) {
        return ItemRegistered.TYPE.equals(eventType)
                || ItemActivated.TYPE.equals(eventType)
                || LocationOpened.TYPE.equals(eventType);
    }

    @Override
    public void onEvent(Event event) {
        // 1. allocate the functional core's input (I/O) - the todo list is this shell's own state
        var domainEvent = BlueprintEvents.MAPPING.decode(event);

        // 2. functional core (pure): react to the event as a trigger, fold it in as a fact
        var commands = ActivateItemsAtOpenedLocationPolicy.react(todoList, domainEvent);
        todoList = ActivateItemsAtOpenedLocationPolicy.evolve(todoList, domainEvent);

        // 3. dispatch the outcome into the target slice's imperative shell
        commands.forEach(activateItem::handle);
    }
}
