/**
 * The build kit's verified reference context: one working example per Event Modeling pattern, each
 * split into a pure {@code funcore} package and an imperative shell. Excluded from the runtime jar.
 *
 * <table>
 *   <caption>Direct sub-packages and the Event Modeling pattern each one demonstrates</caption>
 *   <tr><th>Sub-package</th><th>Pattern</th><th>Example of</th></tr>
 *   <tr><td>{@code events}</td><td>Events</td>
 *       <td>the context's facts and published contract: sealed {@code BlueprintEvent}, tags, the
 *       {@code BlueprintEvents} mapping, {@code @NamedInterface("events")}</td></tr>
 *   <tr><td>{@code registeritem}</td><td>State change</td><td>REST trigger, rejection</td></tr>
 *   <tr><td>{@code activateitem}</td><td>State change</td><td>automation trigger, repeat is a no-op</td></tr>
 *   <tr><td>{@code openlocation}</td><td>State change</td><td>REST trigger, never rejects</td></tr>
 *   <tr><td>{@code items}</td><td>State view</td><td>JPA projection, replay-safe</td></tr>
 *   <tr><td>{@code automation}</td><td>Automation</td>
 *       <td>{@code activateregistereditem}: stateless;
 *       {@code activateitemsatopenedlocation}: private read model (todo list)</td></tr>
 *   <tr><td>{@code translation}</td><td>Translation</td>
 *       <td>{@code facilitysitestatus}: inbound (external message → our command);
 *       {@code reportitemtoassetregistry}: outbound (our event → external system)</td></tr>
 * </table>
 *
 * <p>Each slice package declares its pattern with {@link io.continuum.slices.EventModelingPattern} on
 * its {@code package-info.java}; {@code BlueprintPatternMapTest} fails if a slice doesn't, if a
 * declaration differs from this table, or if a pattern loses its last example. The board's
 * specifications for each slice live in its test-side {@code funcore/*Specification}; the storylines {@code ItemLifecycleStoryline},
 * {@code LocationOpeningStoryline} and {@code SiteGoesLiveStoryline} (test sources, this package)
 * replay the patterns together - {@code SiteGoesLiveStoryline} uses all of them in one flow.
 */
package io.continuum.slices.blueprint;
