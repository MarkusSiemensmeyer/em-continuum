package io.continuum.slices.blueprint.registeritem;

import io.continuum.eventstore.OptimisticConcurrencyException;
import io.continuum.slices.CommandRejectedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP trigger of the RegisterItem slice: translates the request into a command, hands it to the
 * imperative shell, and translates the outcome back - nothing else.
 */
@RestController
@ConditionalOnProperty(prefix = "slices.blueprint.write", name = "registeritem.enabled")
public class RegisterItemRestController {

    private final RegisterItemCommandHandler commandHandler;

    public RegisterItemRestController(RegisterItemCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping("/api/blueprint/items")
    public ResponseEntity<Void> handle(@RequestBody RegisterItemRequestBody body) {
        try {
            commandHandler.handle(new RegisterItemCommand(body.itemId(), body.name(), body.locationId(), body.activateImmediately()));
            return ResponseEntity.ok().build();
        } catch (CommandRejectedException e) {
            // business rule violated by the functional core
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (OptimisticConcurrencyException e) {
            // still conflicting after all retries of the imperative shell
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    public record RegisterItemRequestBody(String itemId, String name, String locationId, boolean activateImmediately) {
    }
}
