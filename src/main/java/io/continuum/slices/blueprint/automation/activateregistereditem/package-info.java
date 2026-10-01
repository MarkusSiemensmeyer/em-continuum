/**
 * ActivateRegisteredItem - a stateless automation: the pure policy maps ItemRegistered to
 * ActivateItem (or nothing), the processor dispatches into ActivateItem's shell.
 */
@EventModelingPattern(value = AUTOMATION, variant = "stateless")
package io.continuum.slices.blueprint.automation.activateregistereditem;

import io.continuum.slices.EventModelingPattern;

import static io.continuum.slices.EventModelingPattern.Type.AUTOMATION;
