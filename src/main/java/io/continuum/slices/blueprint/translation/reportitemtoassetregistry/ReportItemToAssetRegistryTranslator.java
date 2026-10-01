package io.continuum.slices.blueprint.translation.reportitemtoassetregistry;

import io.continuum.eventstore.DecisionModelLoader;
import io.continuum.eventstore.SliceEventListener;
import io.continuum.slices.blueprint.events.BlueprintEvents;
import io.continuum.slices.blueprint.events.EventTags;
import io.continuum.slices.blueprint.events.ItemActivated;
import io.continuum.slices.blueprint.events.ItemReportedToAssetRegistry;
import io.continuum.slices.blueprint.translation.reportitemtoassetregistry.funcore.ReportItemToAssetRegistryTranslation;
import io.umadb.client.Event;
import io.umadb.client.Query;
import io.umadb.client.QueryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * <b>Imperative shell</b> of the outbound ReportItemToAssetRegistry translation: on every
 * {@link ItemActivated}, reads whether the item was reported already, asks the pure translation
 * what to do, delivers the notice to the external {@link AssetRegistry}, and only then records
 * {@link ItemReportedToAssetRegistry}.
 * <ul>
 *   <li><b>Recorded fact instead of in-memory dedup:</b> {@code EventDispatcher} replays from
 *       position 0 on every start; the recorded event makes the core skip items already reported.</li>
 *   <li><b>At least once:</b> a crash between delivering and recording reports the item again on the
 *       next start - the notice's {@code assetId} is the registry's idempotency key.</li>
 *   <li><b>No {@code ConflictRetry}:</b> re-running the attempt would call the registry again. A
 *       conflict on the record means another instance reported the same item - which the registry's
 *       idempotency absorbs; the dispatcher logs it.</li>
 *   <li><b>Registry down:</b> {@link AssetRegistry#report} throws, nothing is recorded, the dispatcher
 *       logs it, and the next start's replay tries again.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "slices.blueprint.translation", name = "reportitemtoassetregistry.enabled")
public class ReportItemToAssetRegistryTranslator implements SliceEventListener {

    private final DecisionModelLoader loader;
    private final AssetRegistry assetRegistry;
    private final Clock clock;

    public ReportItemToAssetRegistryTranslator(DecisionModelLoader loader, AssetRegistry assetRegistry, Clock clock) {
        this.loader = loader;
        this.assetRegistry = assetRegistry;
        this.clock = clock;
    }

    @Override
    public boolean supports(String eventType) {
        return ItemActivated.TYPE.equals(eventType);
    }

    @Override
    public void onEvent(Event event) {
        // 1. allocate the functional core's input (I/O)
        var activated = (ItemActivated) BlueprintEvents.MAPPING.decode(event);
        var boundary = consistencyBoundary(activated.itemId());
        var folded = loader.fold(boundary, BlueprintEvents.MAPPING,
                ReportItemToAssetRegistryTranslation.INITIAL, ReportItemToAssetRegistryTranslation::evolve);
        var now = clock.instant();

        // 2. functional core (pure): our event → their notice + the fact to record
        var report = ReportItemToAssetRegistryTranslation.translate(folded.state(), activated, now);

        // 3. I/O: deliver to the external system first, record the fact only once it accepted
        report.ifPresent(r -> {
            assetRegistry.report(r.notice());
            loader.append(List.of(r.reported()), BlueprintEvents.MAPPING, boundary, folded.lastPosition());
        });
    }

    /** The facts the translation needs: whether this item was reported before. */
    static Query consistencyBoundary(String itemId) {
        return Query.of(QueryItem.of(
                List.of(ItemReportedToAssetRegistry.TYPE),
                List.of(EventTags.tag(EventTags.ITEM_ID, itemId))
        ));
    }
}
