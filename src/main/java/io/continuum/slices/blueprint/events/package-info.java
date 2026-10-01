/**
 * The {@code blueprint} context's published contract: its events. Everything else in the context
 * (commands, shells, functional cores, read models) is internal to its Spring Modulith module;
 * other contexts may only react to what is exposed here.
 */
@NamedInterface("events")
package io.continuum.slices.blueprint.events;

import org.springframework.modulith.NamedInterface;
