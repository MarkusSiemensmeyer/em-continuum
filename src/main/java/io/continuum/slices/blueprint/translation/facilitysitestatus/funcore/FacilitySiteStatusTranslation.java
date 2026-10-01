package io.continuum.slices.blueprint.translation.facilitysitestatus.funcore;

import io.continuum.slices.blueprint.openlocation.OpenLocationCommand;
import io.continuum.slices.blueprint.translation.facilitysitestatus.SiteStatusChanged;

import java.util.List;
import java.util.Locale;

/**
 * <b>Functional core</b> of the inbound FacilitySiteStatus translation - the anti-corruption layer
 * between the facility management system's vocabulary and ours: a site that became
 * {@code OPERATIONAL} is a location to open; every other status means nothing to this context.
 * Dispatching the commands is the shell's job ({@code FacilitySiteStatusTranslator}).
 */
public final class FacilitySiteStatusTranslation {

    static final String OPERATIONAL = "OPERATIONAL";

    private FacilitySiteStatusTranslation() {
    }

    public static List<OpenLocationCommand> translate(SiteStatusChanged message) {
        if (!OPERATIONAL.equalsIgnoreCase(message.status())) {
            return List.of();
        }
        return List.of(new OpenLocationCommand(locationIdOf(message.siteCode())));
    }

    /** Their site code {@code "HAMBURG"} is our location id {@code "loc-hamburg"}. */
    static String locationIdOf(String siteCode) {
        return "loc-" + siteCode.toLowerCase(Locale.ROOT);
    }
}
