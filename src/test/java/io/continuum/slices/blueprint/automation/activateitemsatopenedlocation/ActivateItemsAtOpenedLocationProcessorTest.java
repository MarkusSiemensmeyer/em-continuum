package io.continuum.slices.blueprint.automation.activateitemsatopenedlocation;

import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.activateitem.ActivateItemCommandHandler;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.slices.blueprint.openlocation.OpenLocationCommand;
import io.continuum.slices.blueprint.openlocation.OpenLocationCommandHandler;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommand;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommandHandler;
import io.continuum.testsupport.InMemoryUmaDbClient;
import io.umadb.client.Query;
import io.umadb.client.ReadRequest;
import io.umadb.client.SequencedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Imperative shell of the automation, wired to the REAL shells on one in-memory store (no mocks).
 * {@link #dispatch()} plays {@code EventDispatcher}: it hands every not yet delivered event to the
 * processor in store order - including the events its own commands append.
 */
class ActivateItemsAtOpenedLocationProcessorTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private InMemoryUmaDbClient client;
    private RegisterItemCommandHandler registerItem;
    private OpenLocationCommandHandler openLocation;
    private ActivateItemCommandHandler activateItem;
    private ActivateItemsAtOpenedLocationProcessor processor;
    private long delivered;

    @BeforeEach
    void setUp() {
        client = new InMemoryUmaDbClient();
        var loader = new DecisionModelLoader(client);
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        registerItem = new RegisterItemCommandHandler(loader, clock);
        openLocation = new OpenLocationCommandHandler(loader, clock);
        activateItem = new ActivateItemCommandHandler(loader, clock);
        processor = new ActivateItemsAtOpenedLocationProcessor(activateItem);
        delivered = 0;
    }

    @Test
    @DisplayName("reacts to its setup events and its trigger only")
    void supports() {
        assertThat(processor.supports(ItemRegistered.TYPE)).isTrue();
        assertThat(processor.supports(ItemActivated.TYPE)).isTrue();
        assertThat(processor.supports(LocationOpened.TYPE)).isTrue();
        assertThat(processor.supports("Other.Event")).isFalse();
    }

    @Test
    @DisplayName("when a location opens, its pending items are activated - others stay pending")
    void activatesPendingItemsAtOpenedLocation() {
        registerItem.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", false));
        registerItem.handle(new RegisterItemCommand("item-2", "Valve", "loc-2", false));
        openLocation.handle(new OpenLocationCommand("loc-1"));

        dispatch();

        assertThat(activations()).containsExactly(new ItemActivated("item-1", NOW));
    }

    @Test
    @DisplayName("an item registered after its location opened is not activated by this automation")
    void itemRegisteredAfterOpening() {
        openLocation.handle(new OpenLocationCommand("loc-1"));
        registerItem.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", false));

        dispatch();

        assertThat(activations()).isEmpty();
    }

    @Test
    @DisplayName("a restart replays from position 0 and rebuilds the todo list - still activated only once")
    void replayAfterRestart() {
        registerItem.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", false));
        openLocation.handle(new OpenLocationCommand("loc-1"));
        dispatch();

        // restart: a fresh processor with an empty todo list, fed the whole store again
        processor = new ActivateItemsAtOpenedLocationProcessor(activateItem);
        delivered = 0;
        dispatch();

        assertThat(activations()).hasSize(1);
    }

    /** Delivers every not yet delivered event, in store order, until no new ones appear. */
    private void dispatch() {
        List<SequencedEvent> pending;
        while (!(pending = client.handle(ReadRequest.of(Query.empty()).withStart(delivered)).next().events()).isEmpty()) {
            for (var sequenced : pending) {
                if (processor.supports(sequenced.event().type())) {
                    processor.onEvent(sequenced.event());
                }
                delivered = sequenced.position();
            }
        }
    }

    private List<ItemActivated> activations() {
        return client.handle(ReadRequest.of(Query.empty())).next().events().stream()
                .map(SequencedEvent::event)
                .filter(e -> e.type().equals(ItemActivated.TYPE))
                .map(e -> (ItemActivated) BlueprintEvents.MAPPING.decode(e))
                .toList();
    }
}
