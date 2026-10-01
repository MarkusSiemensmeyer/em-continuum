package io.continuum.slices.blueprint.items;

import io.continuum.slices.blueprint.items.funcore.ItemStatus;
import io.continuum.slices.blueprint.items.funcore.ItemSummary;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Persistence shape of an {@link ItemSummary} - stays in the shell, the core never sees it. */
@Entity
@Table(name = "blueprint_items")
class ItemsEntity {

    @Id
    private String itemId;
    private String name;
    @Enumerated(EnumType.STRING)
    private ItemStatus status;
    private Instant registeredAt;
    private Instant activatedAt;

    protected ItemsEntity() {
    }

    static ItemsEntity from(ItemSummary summary) {
        var entity = new ItemsEntity();
        entity.itemId = summary.itemId();
        entity.name = summary.name();
        entity.status = summary.status();
        entity.registeredAt = summary.registeredAt();
        entity.activatedAt = summary.activatedAt();
        return entity;
    }

    ItemSummary toSummary() {
        return new ItemSummary(itemId, name, status, registeredAt, activatedAt);
    }
}
