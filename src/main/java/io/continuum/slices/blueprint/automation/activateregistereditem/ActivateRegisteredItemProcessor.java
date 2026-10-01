package io.continuum.slices.blueprint.automation.activateregistereditem;

import io.continuum.eventstore.SliceEventListener;
import io.continuum.slices.blueprint.activateitem.ActivateItemCommandHandler;
import io.continuum.slices.blueprint.automation.activateregistereditem.funcore.ActivateRegisteredItemPolicy;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.umadb.client.Event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * <b>Imperative shell</b> of the ActivateRegisteredItem automation: decodes the raw trigger event
 * (I/O), asks the pure {@link ActivateRegisteredItemPolicy} what to do, and dispatches the
 * resulting command into the target slice's own imperative shell.
 * <p>
 * No retry here - the target command handler already retries its own conflicts. And no
 * idempotency guard here either: {@code EventDispatcher} replays from position 0 on every start,
 * and ActivateItem's functional core turns a repeated activation into a no-op.
 */
@Component
@ConditionalOnProperty(prefix = "slices.blueprint.automation", name = "activateregistereditem.enabled")
public class ActivateRegisteredItemProcessor implements SliceEventListener {

    private final ActivateItemCommandHandler activateItem;

    public ActivateRegisteredItemProcessor(ActivateItemCommandHandler activateItem) {
        this.activateItem = activateItem;
    }

    @Override
    public boolean supports(String eventType) {
        return ItemRegistered.TYPE.equals(eventType);
    }

    @Override
    public void onEvent(Event event) {
        // 1. allocate the functional core's input (I/O)
        var trigger = (ItemRegistered) BlueprintEvents.MAPPING.decode(event);

        // 2. functional core (pure)
        var commands = ActivateRegisteredItemPolicy.react(trigger);

        // 3. dispatch the outcome into the target slice's imperative shell
        commands.forEach(activateItem::handle);
    }
}
