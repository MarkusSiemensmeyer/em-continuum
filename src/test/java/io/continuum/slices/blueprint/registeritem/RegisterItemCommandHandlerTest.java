package io.continuum.slices.blueprint.registeritem;

import io.continuum.eventstore.ConflictRetry;
import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.eventstore.OptimisticConcurrencyException;
import io.continuum.slices.CommandRejectedException;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.EventTags;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.testsupport.InMemoryUmaDbClient;
import io.umadb.client.AppendRequest;
import io.umadb.client.AppendResponse;
import io.umadb.client.Query;
import io.umadb.client.ReadRequest;
import io.umadb.client.ReadResponse;
import io.umadb.client.UmaDbException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Imperative shell: the rules themselves are covered by {@code RegisterItemSpecification} - this
 * test proves the I/O wiring (boundary, clock, tags, append) and the conflict retry.
 */
class RegisterItemCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("appends the decided event, tagged with its item and location id")
    void appendsDecidedEvent() {
        var client = new InMemoryUmaDbClient();
        var handler = new RegisterItemCommandHandler(new DecisionModelLoader(client), CLOCK);

        handler.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", true));

        var stored = client.handle(ReadRequest.of(Query.empty())).next().events();
        assertThat(stored).singleElement().satisfies(se -> {
            assertThat(se.event().type()).isEqualTo(ItemRegistered.TYPE);
            assertThat(se.event().tags()).containsExactly(
                    EventTags.tag(EventTags.ITEM_ID, "item-1"),
                    EventTags.tag(EventTags.LOCATION_ID, "loc-1"));
            assertThat(BlueprintEvents.MAPPING.decode(se.event())).isEqualTo(new ItemRegistered("item-1", "Pump", "loc-1", true, NOW));
        });
    }

    @Test
    @DisplayName("given item already registered, rejects without retrying")
    void rejects() {
        var client = new InMemoryUmaDbClient();
        var handler = new RegisterItemCommandHandler(new DecisionModelLoader(client), CLOCK);
        handler.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", true));

        assertThatThrownBy(() -> handler.handle(new RegisterItemCommand("item-1", "Valve", "loc-1", false)))
                .isInstanceOf(CommandRejectedException.class);
    }

    @Test
    @DisplayName("given a concurrent registration between read and append, re-reads and re-decides")
    void retryReDecidesOnFreshFacts() {
        // someone else registers item-1 right before our first append
        var client = new ConcurrentWriterClient(new ItemRegistered("item-1", "Other", "loc-1", false, NOW));
        var handler = new RegisterItemCommandHandler(new DecisionModelLoader(client), CLOCK);

        // the retry re-reads, now sees the registration, and the core rejects before appending
        assertThatThrownBy(() -> handler.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", true)))
                .isInstanceOf(CommandRejectedException.class);
        assertThat(client.reads).hasValue(2);
        assertThat(client.appendAttempts).hasValue(1);
    }

    @Test
    @DisplayName("given a conflict on every attempt, gives up after 5 attempts")
    void givesUpAfterMaxAttempts() {
        var appendAttempts = new AtomicInteger();
        var client = new InMemoryUmaDbClient() {
            @Override
            public synchronized AppendResponse handle(AppendRequest request) {
                appendAttempts.incrementAndGet();
                throw new UmaDbException.IntegrityException("simulated constant contention");
            }
        };
        var handler = new RegisterItemCommandHandler(new DecisionModelLoader(client), CLOCK);

        assertThatThrownBy(() -> handler.handle(new RegisterItemCommand("item-1", "Pump", "loc-1", true)))
                .isInstanceOf(OptimisticConcurrencyException.class);
        assertThat(appendAttempts).hasValue(ConflictRetry.MAX_ATTEMPTS);
    }

    /** Simulates another writer: appends {@code concurrentWrite} just before this handler's first append. */
    static class ConcurrentWriterClient extends InMemoryUmaDbClient {

        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger appendAttempts = new AtomicInteger();
        private final BlueprintEvent concurrentWrite;

        ConcurrentWriterClient(BlueprintEvent concurrentWrite) {
            this.concurrentWrite = concurrentWrite;
        }

        @Override
        public synchronized AppendResponse handle(AppendRequest request) {
            if (appendAttempts.incrementAndGet() == 1) {
                super.handle(new AppendRequest(List.of(BlueprintEvents.MAPPING.encode(concurrentWrite)), null));
            }
            return super.handle(request);
        }

        @Override
        public synchronized Iterator<ReadResponse> handle(ReadRequest request) {
            reads.incrementAndGet();
            return super.handle(request);
        }
    }
}
