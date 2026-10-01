package io.continuum.slices.blueprint.items;

import io.continuum.slices.blueprint.items.funcore.ItemSummary;

import java.util.List;

public record GetItems() {

    public record Result(List<ItemSummary> items) {
    }
}
