package io.continuum.slices.blueprint.translation.facilitysitestatus;

import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.LocationOpened;
import io.continuum.slices.blueprint.openlocation.OpenLocationCommandHandler;
import io.continuum.testsupport.InMemoryUmaDbClient;
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
 * Imperative shell of the inbound translation, wired to the REAL OpenLocation shell on an in-memory
 * store (no mocks) - exactly what the webhook calls.
 */
class FacilitySiteStatusTranslatorTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private InMemoryUmaDbClient client;
    private FacilitySiteStatusTranslator translator;

    @BeforeEach
    void setUp() {
        client = new InMemoryUmaDbClient();
        var openLocation = new OpenLocationCommandHandler(new DecisionModelLoader(client), Clock.fixed(NOW, ZoneOffset.UTC));
        translator = new FacilitySiteStatusTranslator(openLocation);
    }

    @Test
    @DisplayName("an OPERATIONAL site opens our location")
    void operationalOpensLocation() {
        translator.handle(new SiteStatusChanged("HAMBURG", "OPERATIONAL"));

        assertThat(storedEvents()).containsExactly(new LocationOpened("loc-hamburg", NOW));
    }

    @Test
    @DisplayName("any other status appends nothing")
    void otherStatusAppendsNothing() {
        translator.handle(new SiteStatusChanged("HAMBURG", "CLOSED"));

        assertThat(storedEvents()).isEmpty();
    }

    @Test
    @DisplayName("a redelivered message (the sender retries) opens the location only once")
    void redeliveryIsHarmless() {
        translator.handle(new SiteStatusChanged("HAMBURG", "OPERATIONAL"));
        translator.handle(new SiteStatusChanged("HAMBURG", "OPERATIONAL"));

        assertThat(storedEvents()).hasSize(1);
    }

    private List<Object> storedEvents() {
        return client.handle(ReadRequest.of(Query.empty())).next().events().stream()
                .map(se -> (Object) BlueprintEvents.MAPPING.decode(se.event()))
                .toList();
    }
}
