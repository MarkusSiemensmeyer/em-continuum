/**
 * FacilitySiteStatus - an inbound translation: the facility management system's SiteStatusChanged
 * (their vocabulary) becomes OpenLocation (ours); the webhook is just one trigger of the channel-
 * agnostic translator.
 */
@EventModelingPattern(value = TRANSLATION, variant = "inbound (external message → our command)")
package io.continuum.slices.blueprint.translation.facilitysitestatus;

import io.continuum.slices.EventModelingPattern;

import static io.continuum.slices.EventModelingPattern.Type.TRANSLATION;
