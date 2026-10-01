package io.continuum.slices.blueprint.translation.reportitemtoassetregistry.funcore;

import io.continuum.slices.blueprint.events.BlueprintEvent;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemReportedToAssetRegistry;
import io.continuum.slices.blueprint.translation.reportitemtoassetregistry.AssetRegistryNotice;

import java.time.Instant;
import java.util.Optional;

/**
 * <b>Functional core</b> of the outbound ReportItemToAssetRegistry translation: our
 * {@link ItemActivated} in the asset registry's vocabulary - plus the fact to record once the
 * registry accepted it, so a replay of {@code ItemActivated} doesn't report the item again.
 * Sending and recording is the shell's job ({@code ReportItemToAssetRegistryTranslator}).
 */
public final class ReportItemToAssetRegistryTranslation {

    static final String IN_SERVICE = "IN_SERVICE";

    /** Has this item been reported before? */
    public record State(boolean reported) {
    }

    /** What to send to the registry, and what to record after it accepted. */
    public record Report(AssetRegistryNotice notice, ItemReportedToAssetRegistry reported) {
    }

    public static final State INITIAL = new State(false);

    private ReportItemToAssetRegistryTranslation() {
    }

    public static State evolve(State state, BlueprintEvent event) {
        return switch (event) {
            case ItemReportedToAssetRegistry e -> new State(true);
            default -> state;
        };
    }

    public static Optional<Report> translate(State state, ItemActivated event, Instant now) {
        if (state.reported()) {
            return Optional.empty();
        }
        return Optional.of(new Report(
                new AssetRegistryNotice(event.itemId(), IN_SERVICE, event.activatedAt()),
                new ItemReportedToAssetRegistry(event.itemId(), now)));
    }
}
