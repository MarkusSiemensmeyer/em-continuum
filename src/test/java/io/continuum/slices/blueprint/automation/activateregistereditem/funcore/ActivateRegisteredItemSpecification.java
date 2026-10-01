package io.continuum.slices.blueprint.automation.activateregistereditem.funcore;

import io.continuum.slices.blueprint.ItemLifecycleStoryline;
import io.continuum.slices.blueprint.activateitem.ActivateItemCommand;
import io.continuum.slices.blueprint.events.ItemRegistered;
import io.continuum.testsupport.spec.AutomationSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/** ActivateRegisteredItem - the board's specifications, one test each, run against the pure policy. */
class ActivateRegisteredItemSpecification {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private static final AutomationSpecification<ItemRegistered, ActivateItemCommand> SPEC =
            AutomationSpecification.of(ActivateRegisteredItemPolicy::react);

    @Test
    @DisplayName("given ItemRegistered for immediate activation, then ActivateItem")
    void activatesImmediately() {
        SPEC.given(new ItemRegistered("item-1", "Pump", "loc-1", true, NOW))
                .then(new ActivateItemCommand("item-1"));
    }

    @Test
    @DisplayName("given ItemRegistered without immediate activation, then nothing")
    void doesNotActivate() {
        SPEC.given(new ItemRegistered("item-1", "Pump", "loc-1", false, NOW))
                .thenNothing();
    }

    @Nested
    @DisplayName("Storyline: " + ItemLifecycleStoryline.TITLE)
    class ItemLifecycle {

        @Test
        @DisplayName("2 → 4: ItemRegistered triggers ActivateItem")
        void activateRegisteredItem() {
            SPEC.given(ItemLifecycleStoryline.ITEM_REGISTERED)
                    .then(ItemLifecycleStoryline.ACTIVATE_ITEM);
        }
    }
}
