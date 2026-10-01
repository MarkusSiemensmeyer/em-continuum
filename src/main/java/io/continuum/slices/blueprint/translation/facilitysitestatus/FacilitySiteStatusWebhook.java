package io.continuum.slices.blueprint.translation.facilitysitestatus;

import io.continuum.eventstore.OptimisticConcurrencyException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook trigger of the inbound FacilitySiteStatus translation: the facility management system
 * POSTs its {@link SiteStatusChanged} messages here. Any non-2xx answer makes the sender redeliver,
 * which the translation tolerates (see {@link FacilitySiteStatusTranslator}).
 */
@RestController
@ConditionalOnProperty(prefix = "slices.blueprint.translation", name = "facilitysitestatus.enabled")
public class FacilitySiteStatusWebhook {

    private final FacilitySiteStatusTranslator translator;

    public FacilitySiteStatusWebhook(FacilitySiteStatusTranslator translator) {
        this.translator = translator;
    }

    @PostMapping("/api/blueprint/webhooks/facility-management/site-status")
    public ResponseEntity<Void> receive(@RequestBody SiteStatusChanged message) {
        try {
            translator.handle(message);
            return ResponseEntity.accepted().build();
        } catch (OptimisticConcurrencyException e) {
            // still conflicting after all retries of the target shell - the sender redelivers
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
