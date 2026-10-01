/**
 * RegisterItem - a state change triggered over REST. Shows the full write-slice shape: command,
 * pure {@code funcore.RegisterItemDecision} (State, evolve, decide), the imperative shell {@code
 * RegisterItemCommandHandler} (fold, decide, append inside ConflictRetry) and a rule violation
 * rejected as {@code CommandRejectedException} (HTTP 422).
 */
@EventModelingPattern(value = STATE_CHANGE, variant = "REST trigger, rejection")
package io.continuum.slices.blueprint.registeritem;

import io.continuum.slices.EventModelingPattern;

import static io.continuum.slices.EventModelingPattern.Type.STATE_CHANGE;
