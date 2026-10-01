package io.continuum.slices.blueprint.registeritem;

import io.continuum.eventstore.ConflictRetry;
import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.EventTags;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.registeritem.funcore.RegisterItemDecision;
import io.umadb.client.Query;
import io.umadb.client.QueryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * <b>Imperative shell</b> of the RegisterItem slice - the one entry into it for every caller
 * (REST controller, listener, automation). All I/O of the slice happens here and only here:
 * reading the consistency boundary, reading the clock, appending the outcome. The business
 * decision itself is delegated to the pure {@link RegisterItemDecision}.
 * <p>
 * The whole read-decide-append attempt runs inside {@link ConflictRetry#onConflict}: on a
 * concurrent conflicting append it is re-run from scratch, up to 5 times.
 */
@Component
@ConditionalOnProperty(prefix = "slices.blueprint.write", name = "registeritem.enabled")
public class RegisterItemCommandHandler {

    private final DecisionModelLoader loader;
    private final Clock clock;

    public RegisterItemCommandHandler(DecisionModelLoader loader, Clock clock) {
        this.loader = loader;
        this.clock = clock;
    }

    public void handle(RegisterItemCommand command) {
        ConflictRetry.onConflict(() -> {
            // 1. allocate the functional core's input (I/O)
            var boundary = consistencyBoundary(command.itemId());
            var folded = loader.fold(boundary, BlueprintEvents.MAPPING, RegisterItemDecision.INITIAL, RegisterItemDecision::evolve);
            var now = clock.instant();

            // 2. functional core (pure)
            var events = RegisterItemDecision.decide(folded.state(), command, now);

            // 3. append the outcome (I/O), guarded by the same boundary it was decided on
            loader.append(events, BlueprintEvents.MAPPING, boundary, folded.lastPosition());
        });
    }

    /** The facts {@link RegisterItemDecision} needs: prior registrations of this item id. */
    static Query consistencyBoundary(String itemId) {
        return Query.of(QueryItem.of(
                List.of(ItemRegistered.TYPE),
                List.of(EventTags.tag(EventTags.ITEM_ID, itemId))
        ));
    }
}
