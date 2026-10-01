package io.continuum.slices.blueprint.automation.activateregistereditem;

import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.activateitem.ActivateItemCommandHandler;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommand;
import io.continuum.slices.blueprint.registeritem.RegisterItemCommandHandler;
import io.continuum.testsupport.InMemoryUmaDbClient;
import io.umadb.client.Event;
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
 * Imperative shell of the automation, wired to the REAL target shells on one in-memory store
 * (no mocks): RegisterItem appends the trigger, the processor is handed that raw event exactly
 * as {@code EventDispatcher} would, and ActivateItem appends the outcome.
 */
class ActivateRegisteredItemProcessorTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private InMemoryUmaDbClient client;
    private RegisterItemCommandHandler registerItem;
    private ActivateRegisteredItemProcessor processor;

    @BeforeEach
    void setUp() {
        client = new InMemoryUmaDbClient();
        var loader = new DecisionModelLoader(client);
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        registerItem = new RegisterItemCommandHandler(loader, clock);
        processor = new ActivateRegisteredItemProcessor(new ActivateItemCommandHandler(loader, clock));
    }

    @Test
    @DisplayName("only reacts to ItemRegistered")
    void supports() {
        assertThat(processor.supports(ItemRegistered.TYPE)).isTrue();
        assertThat(processor.supports(ItemActivated.TYPE)).isFalse();
    }

    @Test
    @DisplayName("given item registered for immediate activation, then item activated")
    void activates() {
        registerItem.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", true));

        processor.onEvent(lastStoredEvent());

        assertThat(activations()).containsExactly(new ItemActivated("item-1", NOW));
    }

    @Test
    @DisplayName("given item registered without immediate activation, then nothing")
    void doesNotActivate() {
        registerItem.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", false));

        processor.onEvent(lastStoredEvent());

        assertThat(activations()).isEmpty();
    }

    @Test
    @DisplayName("given the trigger is redelivered (dispatcher replay), then still activated only once")
    void replayIsHarmless() {
        registerItem.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", true));
        var trigger = lastStoredEvent();

        processor.onEvent(trigger);
        processor.onEvent(trigger);

        assertThat(activations()).hasSize(1);
    }

    private List<Event> storedEvents() {
        return client.handle(ReadRequest.of(Query.empty())).next().events().stream()
                .map(se -> se.event())
                .toList();
    }

    private Event lastStoredEvent() {
        return storedEvents().getLast();
    }

    private List<ItemActivated> activations() {
        return storedEvents().stream()
                .filter(e -> e.type().equals(ItemActivated.TYPE))
                .map(e -> (ItemActivated) BlueprintEvents.MAPPING.decode(e))
                .toList();
    }
}
