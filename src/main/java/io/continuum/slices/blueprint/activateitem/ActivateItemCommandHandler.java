package io.continuum.slices.blueprint.activateitem;

import io.continuum.eventstore.ConflictRetry;
import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.activateitem.funcore.ActivateItemDecision;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.EventTags;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.umadb.client.Query;
import io.umadb.client.QueryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * <b>Imperative shell</b> of the ActivateItem slice - same shape as
 * {@code RegisterItemCommandHandler}. Entered by the {@code ActivateRegisteredItem} automation,
 * not over HTTP: which trigger calls {@link #handle} makes no difference to the shell.
 */
@Component
@ConditionalOnProperty(prefix = "slices.blueprint.write", name = "activateitem.enabled")
public class ActivateItemCommandHandler {

    private final DecisionModelLoader loader;
    private final Clock clock;

    public ActivateItemCommandHandler(DecisionModelLoader loader, Clock clock) {
        this.loader = loader;
        this.clock = clock;
    }

    public void handle(ActivateItemCommand command) {
        ConflictRetry.onConflict(() -> {
            // 1. allocate the functional core's input (I/O)
            var boundary = consistencyBoundary(command.itemId());
            var folded = loader.fold(boundary, BlueprintEvents.MAPPING, ActivateItemDecision.INITIAL, ActivateItemDecision::evolve);
            var now = clock.instant();

            // 2. functional core (pure)
            var events = ActivateItemDecision.decide(folded.state(), command, now);

            // 3. append the outcome (I/O) - a no-op when the core decided nothing needs to happen
            loader.append(events, BlueprintEvents.MAPPING, boundary, folded.lastPosition());
        });
    }

    /** The facts {@link ActivateItemDecision} needs: this item's registration and activation. */
    static Query consistencyBoundary(String itemId) {
        return Query.of(QueryItem.of(
                List.of(ItemRegistered.TYPE, ItemActivated.TYPE),
                List.of(EventTags.tag(EventTags.ITEM_ID, itemId))
        ));
    }
}
