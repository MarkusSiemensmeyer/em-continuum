package io.continuum.slices.blueprint.translation.facilitysitestatus;

import io.continuum.slices.blueprint.openlocation.OpenLocationCommandHandler;
import io.continuum.slices.blueprint.translation.facilitysitestatus.funcore.FacilitySiteStatusTranslation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * <b>Imperative shell</b> of the inbound FacilitySiteStatus translation - the one entry for every
 * channel the facility management system talks through: the webhook today
 * ({@link FacilitySiteStatusWebhook}), a Kafka listener tomorrow - each just decodes the message and
 * calls {@link #handle}.
 * <p>
 * No retry and no dedup here: OpenLocation's shell retries its own conflicts, and its core turns
 * an already open location into a no-op - so a redelivered message is harmless.
 */
@Component
@ConditionalOnProperty(prefix = "slices.blueprint.translation", name = "facilitysitestatus.enabled")
public class FacilitySiteStatusTranslator {

    private final OpenLocationCommandHandler openLocation;

    public FacilitySiteStatusTranslator(OpenLocationCommandHandler openLocation) {
        this.openLocation = openLocation;
    }

    public void handle(SiteStatusChanged message) {
        // 1. the functional core's input: the external message, as decoded by the trigger

        // 2. functional core (pure): their vocabulary → our commands
        var commands = FacilitySiteStatusTranslation.translate(message);

        // 3. dispatch the outcome into the target slice's imperative shell
        commands.forEach(openLocation::handle);
    }
}
