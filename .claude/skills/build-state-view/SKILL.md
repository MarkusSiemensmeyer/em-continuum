---
name: build-state-view
authors:
  - Martin Dilger
description: >
  Implement read slices (projections + query + REST API + tests) that react to events off the
  shared EventDispatcher subscription, in this project's one established pattern, Functional Core /
  Imperative Shell: pure funcore Projection (evolve: current row + event → new row) and the row
  record → @Component Projector as imperative shell (SliceEventListener: decode, load row via JPA,
  evolve, save; answers the query) → Get{SliceName} query record + RestApi →
  funcore/{SliceName}Specification (one test per board GWT, against the pure projection) + a
  @DataJpaTest shell test. Use when implementing a new read slice / projection from a slice.json
  event model in this project. There is exactly one supported style — do not offer alternatives.
---

# UmaDB — Read Slice (Functional Core / Imperative Shell)

Grounded in the verified reference `slices/blueprint/items/` (ItemRegistered + ItemActivated →
one row per item), compiled and passing under `mvn test`, including the architecture tests.
**When in doubt, open the blueprint file of the same name and copy its shape.**

UmaDB has no read-model concept of its own — a read slice is this project's own convention on the
shared `eventstore` package: `SliceEventListener` is what every projector implements, and
`EventDispatcher` (the ONE shared subscription) picks it up automatically as a Spring bean. **You do
not write a subscription or a Testcontainers test per read slice.**

## The shape — read this first

```
.../slices/{context}/{slicename}/
├── Get{SliceName}.java           ← query record + nested Result
├── {SliceName}Projector.java     ← IMPERATIVE SHELL: decode, load row, evolve, save; answers the query
├── {SliceName}Entity.java        ← JPA entity, package-private - persistence shape, shell only
├── {SliceName}Repository.java    ← package-private JpaRepository
├── {SliceName}RestApi.java       ← trigger (if REST is exposed)
└── funcore/
    ├── {SliceName}Projection.java ← FUNCTIONAL CORE: evolve(Optional<row>, event) → Optional<row>, pure
    └── {Row}.java                 ← the read model row record (+ any enums it uses)
src/test/java/.../slices/{context}/{slicename}/
├── {SliceName}ProjectorTest.java  ← shell wiring: decode, JPA load/save, query (@DataJpaTest, H2)
└── funcore/
    └── {SliceName}Specification.java ← the board's GWTs + storyline beats, against the projection
```

- **The projection decides how a row changes** — pure; `Optional.empty()` means "no row (yet)".
- **The projector does the I/O**: decode the raw event, find the current row, run `evolve`, save.
- **No `ConflictRetry`** — a projection can't hit an append conflict, and the dispatcher feeds each
  listener from one thread in store order.
- **Replay-safe by construction** — the dispatcher replays from position 0 on every start. Set
  fields to absolute values taken from events; never increment/append blindly. Add a replay spec.
- **The projection (`funcore/`) is pure** — `FunctionalCoreTest` fails the build otherwise.
- Read slices are **never feature-flagged** (no `@ConditionalOnProperty`).

## Step 0: Discover Target Project Conventions

> **Comments & description**: each element carries a `comments: string[]` array (board comments)
> and a `description` field — use them as implementation hints, and resolve consumed comments via
> `POST <BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/nodes/<nodeId>/comments/<commentId>/resolve`.

Read `.build-kit/CLAUDE.md`. Every path is rooted at `{basePackage}.slices.{context}.{slicename}`;
resolve `{basePackage}` as documented there. If the slice description or comments contain
`## Implementation Guidelines`, **follow them**.

GWT format for read slices: `Given (events) → Then (information)` — no When. The events in Given
tell you which events the projector reacts to; the information in Then is the expected read model.

## Step 1: Ensure Events Exist

Every event the projector reacts to must already exist in `.../slices/{context}/events/` — create
it first via `build-state-change` Step 2 if not. A read slice never invents its own copy of an
event. **Events from another context** may only be imported from that context's `events` package
(its Spring Modulith named interface) — `ModularityTest` fails the build otherwise.

## Step 2: Functional core — `funcore/`

The row record — every field comes from the read model in slice.json, none invented:

```java
package {basePackage}.slices.{context}.{slicename}.funcore;

/** One row of the {SliceName} read model - what the functional core computes and the query returns. */
public record {Row}(String id, String field1, String field2) {}
```

The projection — one `case` per event the slice reacts to:

```java
package {basePackage}.slices.{context}.{slicename}.funcore;

import {basePackage}.slices.{context}.events.{Context}Event;
import {basePackage}.slices.{context}.events.{CreationEvent};
import {basePackage}.slices.{context}.events.{UpdateEvent};

import java.util.Optional;

/**
 * <b>Functional core</b> of the {SliceName} read slice: how one row changes with each event -
 * pure, no JPA. Loading the current row and saving the new one is the shell's job
 * ({@code {SliceName}Projector}). {@code Optional.empty()} means "no row (yet)".
 */
public final class {SliceName}Projection {

    private {SliceName}Projection() {
    }

    public static Optional<{Row}> evolve(Optional<{Row}> current, {Context}Event event) {
        return switch (event) {
            case {CreationEvent} e -> Optional.of(new {Row}(e.id(), e.field1(), current.map({Row}::field2).orElse(null)));
            case {UpdateEvent} e -> current.map(row -> new {Row}(row.id(), row.field1(), e.field2()));
            default -> current;
        };
    }
}
```

- A creation event builds the row but **keeps fields other events own** (`current.map(...)`), so a
  replay of the creation event doesn't wipe them.
- An update event for a row that doesn't exist yet leaves it absent (`current.map(...)`).
- A "row removed" event returns `Optional.empty()` — the shell deletes it (see Step 3).

## Step 3: Imperative shell

Query record:

```java
package {basePackage}.slices.{context}.{slicename};

import {basePackage}.slices.{context}.{slicename}.funcore.{Row};

import java.util.List;

public record Get{SliceName}(/* filter fields from slice.json, if any */) {

    public record Result(List<{Row}> items) {}
}
```

JPA entity + repository — the persistence shape, never seen by the core:

```java
@Entity
@Table(name = "{context}_{slicename}")
class {SliceName}Entity {

    @Id
    private String id;
    private String field1;
    private String field2;

    protected {SliceName}Entity() {
    }

    static {SliceName}Entity from({Row} row) { /* copy each field */ }

    {Row} toRow() {
        return new {Row}(id, field1, field2);
    }
}

interface {SliceName}Repository extends JpaRepository<{SliceName}Entity, String> {
}
```

Enums from `funcore/` map with `@Enumerated(EnumType.STRING)`. For filtered queries add an indexed
column and a derived query (`List<{SliceName}Entity> findAllBy{FilterField}(String value)`) —
filter in the database, not in Java.

The projector:

```java
package {basePackage}.slices.{context}.{slicename};

import {basePackage}.eventstore.SliceEventListener;
import {basePackage}.slices.{context}.events.{Context}Event;
import {basePackage}.slices.{context}.events.{Context}Events;
import {basePackage}.slices.{context}.events.{CreationEvent};
import {basePackage}.slices.{context}.events.{UpdateEvent};
import {basePackage}.slices.{context}.{slicename}.funcore.{SliceName}Projection;
import io.umadb.client.Event;
import org.springframework.stereotype.Component;

/**
 * <b>Imperative shell</b> of the {SliceName} read slice. All I/O happens here; how a row changes
 * is delegated to the pure {@link {SliceName}Projection}. No ConflictRetry: projections can't hit
 * an append conflict, and the dispatcher feeds every listener from one thread in store order.
 */
@Component
public class {SliceName}Projector implements SliceEventListener {

    private final {SliceName}Repository repository;

    public {SliceName}Projector({SliceName}Repository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(String eventType) {
        return {CreationEvent}.TYPE.equals(eventType) || {UpdateEvent}.TYPE.equals(eventType);
    }

    @Override
    public void onEvent(Event event) {
        // 1. allocate the functional core's input (I/O)
        var domainEvent = {Context}Events.MAPPING.decode(event);
        var current = repository.findById(rowIdOf(domainEvent)).map({SliceName}Entity::toRow);

        // 2. functional core (pure)
        var updated = {SliceName}Projection.evolve(current, domainEvent);

        // 3. persist the outcome (I/O)
        updated.map({SliceName}Entity::from).ifPresent(repository::save);
    }

    public Get{SliceName}.Result handle(Get{SliceName} query) {
        return new Get{SliceName}.Result(repository.findAll().stream().map({SliceName}Entity::toRow).toList());
    }

    /** Which row an event belongs to - the read model's key, a persistence concern. */
    private static String rowIdOf({Context}Event event) {
        return switch (event) {
            case {CreationEvent} e -> e.id();
            case {UpdateEvent} e -> e.id();
            default -> throw new IllegalArgumentException("Not projected: " + event);
        };
    }
}
```

If the projection can remove a row, persist that too:
`updated.map(...).ifPresentOrElse(repository::save, () -> repository.deleteById(id))`.

REST API (if exposed) — a trigger that only calls the projector's query:

```java
@RestController
public class {SliceName}RestApi {

    private final {SliceName}Projector projector;

    public {SliceName}RestApi({SliceName}Projector projector) {
        this.projector = projector;
    }

    @GetMapping("/api/{context}/{resource}")
    public Get{SliceName}.Result query() {
        return projector.handle(new Get{SliceName}());
    }
}
```

Plain Spring MVC, not WebFlux. If the read model contains fields the caller already knows from the
query (e.g. the filter), leave them out of `Result`; otherwise return the rows directly.

## Step 4: Specification — `funcore/{SliceName}Specification` (the board's GWTs)

**One test method per `specifications[]` entry**, `@DisplayName` = the spec's title, written with
the shared `ProjectionSpecification` DSL (`testsupport/spec/`) against the pure projection:

```java
package {basePackage}.slices.{context}.{slicename}.funcore;

import {basePackage}.slices.{context}.events.{Context}Event;
import {basePackage}.slices.{context}.events.{CreationEvent};
import {basePackage}.testsupport.spec.ProjectionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {SliceName} - the board's specifications, one test each, run against the pure projection. */
class {SliceName}Specification {

    private static final ProjectionSpecification<{Row}, {Context}Event> SPEC =
            ProjectionSpecification.of({SliceName}Projection::evolve);

    @Test
    @DisplayName("{spec title from slice.json}")
    void empty() {
        SPEC.given().thenNothing();
    }

    @Test
    @DisplayName("{spec title from slice.json}")
    void created() {
        SPEC.given(new {CreationEvent}("id-1", "value1"))
                .then(new {Row}("id-1", "value1", null));
    }
}
```

| GWT element | DSL |
|---|---|
| Given NOTHING | `SPEC.given()` |
| Given event(s) | `SPEC.given(event1, event2)` — folded with `evolve`, in order, from no row |
| Then the row | `.then(expectedRow)` |
| Then empty / no entry | `.thenNothing()` |

The DSL checks one row. When a specification's Then lists several rows for different ids, write
one spec method per row from the events of that id, or assert the query in the shell test (Step 5).
Always add one **replay** spec (all given events twice → same row), named after the scenario it
replays — it guards the dispatcher's restart behaviour.

## Step 4b: Storyline beats (optional, only if `storylines[]` is present)

Beats live once per storyline in `src/test/java/<basePackage>/slices/{context}/{StorylineTitle}Storyline.java`
(create or extend it — see `build-state-change` Step 7b and `blueprint/ItemLifecycleStoryline`); the
read model beats are `{Row}` constants. For every two `READMODEL` beats of this read model with only
`EVENT` beats between them — and for the first `READMODEL` beat after its events — add a test to a
`@Nested @DisplayName("Storyline: " + {StorylineTitle}Storyline.TITLE) class {StorylineTitle}` (one
per storyline): given = all `EVENT` beats up to the later read-model beat, then = that beat.
Reference beats qualified (`{StorylineTitle}Storyline.BEAT`), never via a wildcard static import.

```java
@Test
@DisplayName("3 → 5 → 6: after {UpdateEvent}, {SliceName} shows ...")
void beat() {
    SPEC.given({StorylineTitle}Storyline.{EVENT_BEAT_2}, {StorylineTitle}Storyline.{EVENT_BEAT_5})
            .then({StorylineTitle}Storyline.{READMODEL_BEAT_6});
}
```

If a `COMMAND` sits between two read-model beats, that half belongs to `build-state-change`.

## Step 5: Shell test — `{SliceName}ProjectorTest`

`@DataJpaTest` (Boot 4: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`) gives
a real repository on embedded H2, no full application context. Feed raw events exactly as
`EventDispatcher` would. Copy `ItemsProjectorTest` and adapt — at minimum:

1. `supports` answers true for exactly the projected event types.
2. Events for several rows → `handle(new Get{SliceName}())` returns all rows
   (`containsExactlyInAnyOrder`).
3. The same events delivered twice → rows unchanged.

```java
private void deliver({Context}Event event) {
    projector.onEvent({Context}Events.MAPPING.encode(event));
}
```

## Final Verification

- [ ] Every field of the read model in slice.json is in `{Row}` — none invented, none missing
- [ ] Every event type in slice.json has a `case` in the projection and is answered by `supports`
- [ ] Every `specifications[]` scenario has a test method in `funcore/{SliceName}Specification`, plus one replay spec
- [ ] If `storylines[]` is present: every READMODEL beat pair (only EVENT beats between) has a `@Nested` storyline test — or was deliberately skipped as untraceable
- [ ] No extra query parameters or filter logic beyond slice.json
- [ ] `funcore/` holds only the pure projection and row types; the projector does all I/O
- [ ] `./mvnw compile -q`, then the slice's tests **and** `FunctionalCoreTest` + `ModularityTest`
- [ ] If checks pass, commit with `feat: {Slice Name}` and set slice status to `Done`
