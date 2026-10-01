package io.continuum.slices.blueprint.translation.facilitysitestatus;

/**
 * Message of the EXTERNAL facility management system, in ITS vocabulary - site codes like
 * {@code "HAMBURG"}, statuses like {@code "OPERATIONAL"}. Plain record, no annotations: it is input
 * to the functional core, which translates it into our own commands.
 */
public record SiteStatusChanged(String siteCode, String status) {
}
