package io.continuum.slices.blueprint.items;

import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.items.funcore.ItemStatus;
import io.continuum.slices.blueprint.items.funcore.ItemSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Imperative shell: the row logic itself is covered by {@code ItemsSpecification} - this test
 * proves decoding, load/save through JPA (embedded H2) and the query, fed with raw events exactly
 * as {@code EventDispatcher} would.
 */
@DataJpaTest
class ItemsProjectorTest {

    private static final Instant REGISTERED = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant ACTIVATED = Instant.parse("2026-10-01T10:05:00Z");

    @Autowired
    private ItemsRepository repository;

    private ItemsProjector projector;

    @BeforeEach
    void setUp() {
        projector = new ItemsProjector(repository);
    }

    @Test
    @DisplayName("reacts to ItemRegistered and ItemActivated only")
    void supports() {
        assertThat(projector.supports(ItemRegistered.TYPE)).isTrue();
        assertThat(projector.supports(ItemActivated.TYPE)).isTrue();
        assertThat(projector.supports("Other.Event")).isFalse();
    }

    @Test
    @DisplayName("persists each item's row and returns all rows")
    void persistsAndQueries() {
        deliver(new ItemRegistered("item-1", "Pump", "loc-1", true, REGISTERED));
        deliver(new ItemRegistered("item-2", "Valve", "loc-1", false, REGISTERED));
        deliver(new ItemActivated("item-1", ACTIVATED));

        assertThat(projector.handle(new GetItems()).items()).containsExactlyInAnyOrder(
                new ItemSummary("item-1", "Pump", ItemStatus.ACTIVE, REGISTERED, ACTIVATED),
                new ItemSummary("item-2", "Valve", ItemStatus.REGISTERED, REGISTERED, null));
    }

    @Test
    @DisplayName("a replay from position 0 leaves the rows unchanged")
    void replay() {
        var registered = new ItemRegistered("item-1", "Pump", "loc-1", true, REGISTERED);
        var activated = new ItemActivated("item-1", ACTIVATED);
        deliver(registered);
        deliver(activated);

        deliver(registered);
        deliver(activated);

        assertThat(projector.handle(new GetItems()).items()).containsExactly(
                new ItemSummary("item-1", "Pump", ItemStatus.ACTIVE, REGISTERED, ACTIVATED));
    }

    private void deliver(BlueprintEvent event) {
        projector.onEvent(BlueprintEvents.MAPPING.encode(event));
    }
}
