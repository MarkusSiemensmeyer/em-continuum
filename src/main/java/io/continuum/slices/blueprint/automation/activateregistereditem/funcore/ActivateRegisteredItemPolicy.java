package io.continuum.slices.blueprint.automation.activateregistereditem.funcore;

import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.events.ItemRegistered;

import java.util.List;

/**
 * <b>Functional core</b> of the ActivateRegisteredItem automation: pure event-to-command mapping.
 * Decides WHETHER to react and WHICH commands to send - an empty list means "nothing". Dispatching
 * them is the shell's job ({@code ActivateRegisteredItemProcessor}).
 */
public final class ActivateRegisteredItemPolicy {

    private ActivateRegisteredItemPolicy() {
    }

    public static List<ActivateItemCommand> react(ItemRegistered event) {
        if (!event.activateImmediately()) {
            return List.of();
        }
        return List.of(new ActivateItemCommand(event.itemId()));
    }
}
