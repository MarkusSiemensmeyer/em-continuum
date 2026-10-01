package io.continuum.slices.blueprint.translation.facilitysitestatus.funcore;

import io.continuum.slices.blueprint.SiteGoesLiveStoryline;
import io.continuum.slices.blueprint.openlocation.OpenLocationCommand;
import io.continuum.slices.blueprint.translation.facilitysitestatus.SiteStatusChanged;
import io.continuum.testsupport.spec.AutomationSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * FacilitySiteStatus (inbound translation) - the board's specifications, one test each, run against
 * the pure translation: given the external message, then our command (or nothing).
 */
class FacilitySiteStatusSpecification {

    private static final AutomationSpecification<SiteStatusChanged, OpenLocationCommand> SPEC =
            AutomationSpecification.of(FacilitySiteStatusTranslation::translate);

    @Test
    @DisplayName("given site status OPERATIONAL, then OpenLocation for the matching location")
    void operationalSiteOpensLocation() {
        SPEC.given(new SiteStatusChanged("BERLIN", "OPERATIONAL"))
                .then(new OpenLocationCommand("loc-berlin"));
    }

    @Test
    @DisplayName("given any other site status, then nothing")
    void otherStatusMeansNothing() {
        SPEC.given(new SiteStatusChanged("BERLIN", "UNDER_CONSTRUCTION"))
                .thenNothing();
    }

    @Nested
    @DisplayName("Storyline: " + SiteGoesLiveStoryline.TITLE)
    class SiteGoesLive {

        @Test
        @DisplayName("3 → 4: the site operational message translates into OpenLocation")
        void translateSiteStatus() {
            SPEC.given(SiteGoesLiveStoryline.SITE_OPERATIONAL)
                    .then(SiteGoesLiveStoryline.OPEN_LOCATION);
        }
    }
}
