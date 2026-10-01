package io.continuum.slices.blueprint.translation.reportitemtoassetregistry;

import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemReportedToAssetRegistry;
import io.continuum.testsupport.InMemoryUmaDbClient;
import io.umadb.client.Query;
import io.umadb.client.ReadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Imperative shell of the outbound translation, on an in-memory store and an in-memory
 * {@link AssetRegistry} fake (no mocks): proves the order "deliver, then record" and what it means
 * for replays and an unreachable registry.
 */
class ReportItemToAssetRegistryTranslatorTest {

    private static final Instant ACTIVATED = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-01T10:00:05Z");
    private static final ItemActivated ITEM_ACTIVATED = new ItemActivated("item-1", ACTIVATED);

    private InMemoryUmaDbClient client;
    private FakeAssetRegistry registry;
    private ReportItemToAssetRegistryTranslator translator;

    @BeforeEach
    void setUp() {
        client = new InMemoryUmaDbClient();
        registry = new FakeAssetRegistry();
        translator = new ReportItemToAssetRegistryTranslator(
                new DecisionModelLoader(client), registry, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("only reacts to ItemActivated")
    void supports() {
        assertThat(translator.supports(ItemActivated.TYPE)).isTrue();
        assertThat(translator.supports(ItemReportedToAssetRegistry.TYPE)).isFalse();
    }

    @Test
    @DisplayName("delivers the notice to the registry, then records the item as reported")
    void deliversThenRecords() {
        translator.onEvent(BlueprintEvents.MAPPING.encode(ITEM_ACTIVATED));

        assertThat(registry.received).containsExactly(new AssetRegistryNotice("item-1", "IN_SERVICE", ACTIVATED));
        assertThat(storedEvents()).containsExactly(new ItemReportedToAssetRegistry("item-1", NOW));
    }

    @Test
    @DisplayName("a replay of ItemActivated (dispatcher restart) doesn't report the item again")
    void replayDoesNotReportAgain() {
        translator.onEvent(BlueprintEvents.MAPPING.encode(ITEM_ACTIVATED));
        translator.onEvent(BlueprintEvents.MAPPING.encode(ITEM_ACTIVATED));

        assertThat(registry.received).hasSize(1);
        assertThat(storedEvents()).hasSize(1);
    }

    @Test
    @DisplayName("an unreachable registry records nothing - so the next replay delivers it")
    void unreachableRegistryRecordsNothing() {
        registry.reachable = false;

        assertThatThrownBy(() -> translator.onEvent(BlueprintEvents.MAPPING.encode(ITEM_ACTIVATED)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(storedEvents()).isEmpty();

        registry.reachable = true;
        translator.onEvent(BlueprintEvents.MAPPING.encode(ITEM_ACTIVATED));

        assertThat(registry.received).hasSize(1);
        assertThat(storedEvents()).containsExactly(new ItemReportedToAssetRegistry("item-1", NOW));
    }

    private List<Object> storedEvents() {
        return client.handle(ReadRequest.of(Query.empty())).next().events().stream()
                .map(se -> (Object) BlueprintEvents.MAPPING.decode(se.event()))
                .toList();
    }

    /** In-memory stand-in for the external asset registry. */
    static class FakeAssetRegistry implements AssetRegistry {

        final List<AssetRegistryNotice> received = new ArrayList<>();
        boolean reachable = true;

        @Override
        public void report(AssetRegistryNotice notice) {
            if (!reachable) {
                throw new IllegalStateException("asset registry unreachable");
            }
            received.add(notice);
        }
    }
}
