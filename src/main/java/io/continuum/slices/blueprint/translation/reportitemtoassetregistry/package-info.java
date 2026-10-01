/**
 * ReportItemToAssetRegistry - an outbound translation: ItemActivated becomes an
 * AssetRegistryNotice delivered through a port, then recorded as ItemReportedToAssetRegistry so
 * replays don't re-send (at least once).
 */
@EventModelingPattern(value = TRANSLATION, variant = "outbound (our event → external system)")
package io.continuum.slices.blueprint.translation.reportitemtoassetregistry;

import io.continuum.slices.EventModelingPattern;

import static io.continuum.slices.EventModelingPattern.Type.TRANSLATION;
