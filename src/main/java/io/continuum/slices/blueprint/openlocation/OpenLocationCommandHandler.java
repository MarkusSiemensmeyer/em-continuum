package io.continuum.slices.blueprint.openlocation;

import io.continuum.eventstore.ConflictRetry;
import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.EventTags;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.slices.blueprint.openlocation.funcore.OpenLocationDecision;
import io.umadb.client.Query;
import io.umadb.client.QueryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * <b>Imperative shell</b> of the OpenLocation slice - same shape as
 * {@code RegisterItemCommandHandler}.
 */
@Component
@ConditionalOnProperty(prefix = "slices.blueprint.write", name = "openlocation.enabled")
public class OpenLocationCommandHandler {

    private final DecisionModelLoader loader;
    private final Clock clock;

    public OpenLocationCommandHandler(DecisionModelLoader loader, Clock clock) {
        this.loader = loader;
        this.clock = clock;
    }

    public void handle(OpenLocationCommand command) {
        ConflictRetry.onConflict(() -> {
            // 1. allocate the functional core's input (I/O)
            var boundary = consistencyBoundary(command.locationId());
            var folded = loader.fold(boundary, BlueprintEvents.MAPPING, OpenLocationDecision.INITIAL, OpenLocationDecision::evolve);
            var now = clock.instant();

            // 2. functional core (pure)
            var events = OpenLocationDecision.decide(folded.state(), command, now);

            // 3. append the outcome (I/O) - a no-op when the location is already open
            loader.append(events, BlueprintEvents.MAPPING, boundary, folded.lastPosition());
        });
    }

    /** The facts {@link OpenLocationDecision} needs: whether this location was opened before. */
    static Query consistencyBoundary(String locationId) {
        return Query.of(QueryItem.of(
                List.of(LocationOpened.TYPE),
                List.of(EventTags.tag(EventTags.LOCATION_ID, locationId))
        ));
    }
}
