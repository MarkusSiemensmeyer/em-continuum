package io.continuum.slices.blueprint.items;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemsRestApi {

    private final ItemsProjector projector;

    public ItemsRestApi(ItemsProjector projector) {
        this.projector = projector;
    }

    @GetMapping("/api/blueprint/items")
    public GetItems.Result query() {
        return projector.handle(new GetItems());
    }
}
