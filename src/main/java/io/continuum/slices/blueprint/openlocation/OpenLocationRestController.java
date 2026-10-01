package io.continuum.slices.blueprint.openlocation;

import io.continuum.eventstore.OptimisticConcurrencyException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP trigger of the OpenLocation slice. OpenLocation's core never rejects - opening an open
 * location is a no-op - so there is no 422 branch here.
 */
@RestController
@ConditionalOnProperty(prefix = "slices.blueprint.write", name = "openlocation.enabled")
public class OpenLocationRestController {

    private final OpenLocationCommandHandler commandHandler;

    public OpenLocationRestController(OpenLocationCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping("/api/blueprint/locations/{locationId}/open")
    public ResponseEntity<Void> handle(@PathVariable String locationId) {
        try {
            commandHandler.handle(new OpenLocationCommand(locationId));
            return ResponseEntity.ok().build();
        } catch (OptimisticConcurrencyException e) {
            // still conflicting after all retries of the imperative shell
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
