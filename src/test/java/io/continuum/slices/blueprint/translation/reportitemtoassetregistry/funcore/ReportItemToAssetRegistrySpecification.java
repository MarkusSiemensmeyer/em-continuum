package io.continuum.slices.blueprint.translation.reportitemtoassetregistry.funcore;

import io.continuum.slices.blueprint.SiteGoesLiveStoryline;
import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemReportedToAssetRegistry;
import io.continuum.slices.blueprint.translation.reportitemtoassetregistry.AssetRegistryNotice;
import io.continuum.slices.blueprint.translation.reportitemtoassetregistry.funcore.ReportItemToAssetRegistryTranslation.Report;
import io.continuum.testsupport.spec.TranslationSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * ReportItemToAssetRegistry (outbound translation) - the board's specifications, one test each, run
 * against the pure translation.
 */
class ReportItemToAssetRegistrySpecification {

    private static final Instant ACTIVATED = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-01T10:00:05Z");

    private static final TranslationSpecification<ReportItemToAssetRegistryTranslation.State, BlueprintEvent, ItemActivated, Report> SPEC =
            TranslationSpecification.of(ReportItemToAssetRegistryTranslation.INITIAL, ReportItemToAssetRegistryTranslation::evolve,
                    (state, event) -> ReportItemToAssetRegistryTranslation.translate(state, event, NOW));

    @Test
    @DisplayName("given nothing, when ItemActivated, then the registry is told the asset is IN_SERVICE")
    void reportsActivatedItem() {
        SPEC.given()
                .when(new ItemActivated("item-1", ACTIVATED))
                .then(new Report(
                        new AssetRegistryNotice("item-1", "IN_SERVICE", ACTIVATED),
                        new ItemReportedToAssetRegistry("item-1", NOW)));
    }

    @Test
    @DisplayName("given ItemReportedToAssetRegistry, when ItemActivated (replayed), then nothing")
    void doesNotReportTwice() {
        SPEC.given(new ItemReportedToAssetRegistry("item-1", NOW))
                .when(new ItemActivated("item-1", ACTIVATED))
                .thenNothing();
    }

    @Nested
    @DisplayName("Storyline: " + SiteGoesLiveStoryline.TITLE)
    class SiteGoesLive {

        private static final TranslationSpecification<ReportItemToAssetRegistryTranslation.State, BlueprintEvent, ItemActivated, Report> SPEC =
                TranslationSpecification.of(ReportItemToAssetRegistryTranslation.INITIAL, ReportItemToAssetRegistryTranslation::evolve,
                        (state, event) -> ReportItemToAssetRegistryTranslation.translate(state, event, SiteGoesLiveStoryline.REPORTED_AT));

        @Test
        @DisplayName("7 → 8 → 9: ItemActivated is reported to the registry, then recorded")
        void reportToAssetRegistry() {
            SPEC.given()
                    .when(SiteGoesLiveStoryline.ITEM_ACTIVATED)
                    .then(new Report(SiteGoesLiveStoryline.ASSET_IN_SERVICE, SiteGoesLiveStoryline.ITEM_REPORTED));
        }
    }
}
