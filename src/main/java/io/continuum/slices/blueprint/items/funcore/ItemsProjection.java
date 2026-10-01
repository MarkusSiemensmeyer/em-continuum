package io.continuum.slices.blueprint.items.funcore;

import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;

import java.util.Optional;

/**
 * <b>Functional core</b> of the Items read slice: how one item's row changes with each event -
 * pure, no JPA. Loading the current row and saving the new one is the shell's job
 * ({@code ItemsProjector}).
 * <p>
 * {@code Optional.empty()} means "no row (yet)". Every event sets its fields to absolute values
 * rather than incrementing anything, so replaying the same event (the dispatcher replays from
 * position 0 on every start) yields the same row.
 */
public final class ItemsProjection {

    private ItemsProjection() {
    }

    public static Optional<ItemSummary> evolve(Optional<ItemSummary> current, BlueprintEvent event) {
        return switch (event) {
            case ItemRegistered e -> Optional.of(new ItemSummary(
                    e.itemId(),
                    e.name(),
                    current.map(ItemSummary::status).orElse(ItemStatus.REGISTERED),
                    e.registeredAt(),
                    current.map(ItemSummary::activatedAt).orElse(null)));
            case ItemActivated e -> current.map(item -> new ItemSummary(
                    item.itemId(), item.name(), ItemStatus.ACTIVE, item.registeredAt(), e.activatedAt()));
            default -> current;
        };
    }
}
