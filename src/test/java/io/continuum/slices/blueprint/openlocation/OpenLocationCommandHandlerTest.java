package io.continuum.slices.blueprint.openlocation;

import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.EventTags;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.testsupport.InMemoryUmaDbClient;
import io.umadb.client.Query;
import io.umadb.client.ReadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Imperative shell: the rules themselves are covered by {@code OpenLocationSpecification} - this
 * test proves the I/O wiring, including that an empty decision appends nothing.
 */
class OpenLocationCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private InMemoryUmaDbClient client;
    private OpenLocationCommandHandler handler;

    @BeforeEach
    void setUp() {
        client = new InMemoryUmaDbClient();
        handler = new OpenLocationCommandHandler(new DecisionModelLoader(client), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("appends LocationOpened, tagged with its location id")
    void appendsDecidedEvent() {
        handler.handle(new OpenLocationCommand("loc-1"));

        var stored = client.handle(ReadRequest.of(Query.empty())).next().events();
        assertThat(stored).singleElement().satisfies(se -> {
            assertThat(se.event().tags()).containsExactly(EventTags.tag(EventTags.LOCATION_ID, "loc-1"));
            assertThat(BlueprintEvents.MAPPING.decode(se.event())).isEqualTo(new LocationOpened("loc-1", NOW));
        });
    }

    @Test
    @DisplayName("opening an open location appends nothing")
    void repeatIsNoOp() {
        handler.handle(new OpenLocationCommand("loc-1"));
        handler.handle(new OpenLocationCommand("loc-1"));

        assertThat(client.getHeadPosition()).isEqualTo(1);
    }
}
