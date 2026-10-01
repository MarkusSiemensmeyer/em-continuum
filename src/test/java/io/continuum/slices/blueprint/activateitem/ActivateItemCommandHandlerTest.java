package io.continuum.slices.blueprint.activateitem;

import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.testsupport.InMemoryUmaDbClient;
import io.umadb.client.AppendRequest;
import io.umadb.client.Query;
import io.umadb.client.ReadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Imperative shell: the rules themselves are covered by {@code ActivateItemSpecification} - this
 * test proves the I/O wiring, including that an empty decision appends nothing.
 */
class ActivateItemCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:05:00Z");

    private InMemoryUmaDbClient client;
    private ActivateItemCommandHandler handler;

    @BeforeEach
    void setUp() {
        client = new InMemoryUmaDbClient();
        handler = new ActivateItemCommandHandler(new DecisionModelLoader(client), Clock.fixed(NOW, ZoneOffset.UTC));
        // given: the item was registered (by RegisterItem's shell, in real life)
        client.handle(new AppendRequest(List.of(BlueprintEvents.MAPPING.encode(
                new ItemRegistered("item-1", "Pump", "loc-1", true, NOW))), null));
    }

    @Test
    @DisplayName("appends ItemActivated for a registered item")
    void appendsDecidedEvent() {
        handler.handle(new ActivateItemCommand("item-1"));

        assertThat(storedEvents()).containsExactly(
                new ItemRegistered("item-1", "Pump", "loc-1", true, NOW),
                new ItemActivated("item-1", NOW));
    }

    @Test
    @DisplayName("a repeated activation appends nothing")
    void repeatIsNoOp() {
        handler.handle(new ActivateItemCommand("item-1"));
        handler.handle(new ActivateItemCommand("item-1"));

        assertThat(storedEvents()).filteredOn(ItemActivated.class::isInstance).hasSize(1);
    }

    private List<Object> storedEvents() {
        return client.handle(ReadRequest.of(Query.empty())).next().events().stream()
                .map(se -> (Object) BlueprintEvents.MAPPING.decode(se.event()))
                .toList();
    }
}
