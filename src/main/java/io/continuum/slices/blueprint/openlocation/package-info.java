/**
 * OpenLocation - a state change whose core never rejects: opening an open location decides no
 * events. Its LocationOpened is the trigger of the todo-list automation.
 */
@EventModelingPattern(value = STATE_CHANGE, variant = "REST trigger, never rejects")
package io.continuum.slices.blueprint.openlocation;

import io.continuum.slices.EventModelingPattern;

import static io.continuum.slices.EventModelingPattern.Type.STATE_CHANGE;
