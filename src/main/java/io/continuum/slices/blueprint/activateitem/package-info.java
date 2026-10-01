/**
 * ActivateItem - a state change with no REST trigger: it is entered only by automations. Shows
 * the third decide outcome besides events and rejection - nothing to do (already activated) -
 * which keeps replays from position 0 silent.
 */
@EventModelingPattern(value = STATE_CHANGE, variant = "automation trigger, repeat is a no-op")
package io.continuum.slices.blueprint.activateitem;

import io.continuum.slices.EventModelingPattern;

import static io.continuum.slices.EventModelingPattern.Type.STATE_CHANGE;
