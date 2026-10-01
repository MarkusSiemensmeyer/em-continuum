package io.continuum.slices.blueprint.items;

import io.continuum.eventstore.SliceEventListener;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.items.funcore.ItemsProjection;
import io.umadb.client.Event;
import org.springframework.stereotype.Component;

/**
 * <b>Imperative shell</b> of the Items read slice. All I/O happens here: decoding the raw event,
 * loading the current row, saving the new one, answering queries. How a row changes is delegated
 * to the pure {@link ItemsProjection}.
 * <p>
 * No {@code ConflictRetry} here: projections can't hit an append conflict, and
 * {@code EventDispatcher} feeds every listener from one thread in store order, so there is no
 * concurrent writer to the same row.
 */
@Component
public class ItemsProjector implements SliceEventListener {

    private final ItemsRepository repository;

    public ItemsProjector(ItemsRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(String eventType) {
        return ItemRegistered.TYPE.equals(eventType) || ItemActivated.TYPE.equals(eventType);
    }

    @Override
    public void onEvent(Event event) {
        // 1. allocate the functional core's input (I/O)
        var domainEvent = BlueprintEvents.MAPPING.decode(event);
        var current = repository.findById(itemIdOf(domainEvent)).map(ItemsEntity::toSummary);

        // 2. functional core (pure)
        var updated = ItemsProjection.evolve(current, domainEvent);

        // 3. persist the outcome (I/O)
        updated.map(ItemsEntity::from).ifPresent(repository::save);
    }

    public GetItems.Result handle(GetItems query) {
        return new GetItems.Result(repository.findAll().stream().map(ItemsEntity::toSummary).toList());
    }

    /** Which row an event belongs to - the read model's key, a persistence concern. */
    private static String itemIdOf(BlueprintEvent event) {
        return switch (event) {
            case ItemRegistered e -> e.itemId();
            case ItemActivated e -> e.itemId();
            default -> throw new IllegalArgumentException("Not projected by Items: " + event);
        };
    }
}
