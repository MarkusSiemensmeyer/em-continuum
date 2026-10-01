package io.continuum.slices.blueprint.registeritem;

public record RegisterItemCommand(String itemId, String name, String locationId, boolean activateImmediately) {
}
