/**
 * Items - a state view: the pure {@code funcore.ItemsProjection} evolves one row per event, the
 * shell {@code ItemsProjector} loads and saves it via JPA and answers {@code GetItems}.
 */
@EventModelingPattern(value = STATE_VIEW, variant = "JPA projection, replay-safe")
package io.continuum.slices.blueprint.items;

import io.continuum.slices.EventModelingPattern;

import static io.continuum.slices.EventModelingPattern.Type.STATE_VIEW;
