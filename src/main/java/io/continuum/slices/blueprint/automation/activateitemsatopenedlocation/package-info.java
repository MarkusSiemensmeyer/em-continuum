/**
 * ActivateItemsAtOpenedLocation - an automation with a private read model: setup events keep a
 * todo list (pure evolve), the trigger LocationOpened reads it (pure react); the processor holds
 * the list in memory, rebuilt by every replay.
 */
@EventModelingPattern(value = AUTOMATION, variant = "private read model (todo list)")
package io.continuum.slices.blueprint.automation.activateitemsatopenedlocation;

import io.continuum.slices.EventModelingPattern;

import static io.continuum.slices.EventModelingPattern.Type.AUTOMATION;
