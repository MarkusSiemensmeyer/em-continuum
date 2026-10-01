---
name: build-state-change
authors:
  - Martin Dilger
description: >
  Implement DCB-style write slices against the raw UmaDB Java client in this project's one
  established pattern, Functional Core / Imperative Shell: Command record → pure funcore Decision
  (State record + evolve + decide) → @Component CommandHandler as imperative shell (all I/O, wrapped
  in ConflictRetry) → optional RestController trigger → funcore/{SliceName}Specification (one test
  per board GWT, run against the pure core) + a thin shell test on InMemoryUmaDbClient. Use when
  implementing a new write slice / command handler from a slice.json event model in this project.
  There is exactly one supported style — do not offer alternatives.
---

# UmaDB — Write Slice (Functional Core / Imperative Shell)

One pattern only. Every step is grounded in the verified reference context
`src/main/java/<basePackage>/slices/blueprint/` — `registeritem` (REST-triggered) and
`activateitem` (automation-triggered, with a "nothing to do" outcome), `openlocation` (never
rejects: a repeat is a no-op) — compiled and passing under
`mvn test`, including the architecture tests. **When in doubt, open the blueprint file of the same
name and copy its shape.**

## The shape — read this first

```
.../slices/{context}/{slicename}/
├── {SliceName}Command.java            ← record, plain data (the trigger's input)
├── {SliceName}CommandHandler.java     ← IMPERATIVE SHELL: all I/O, ConflictRetry, the one entry point
├── {SliceName}RestController.java     ← trigger (only if a SCREEN depends on the command)
└── funcore/
    └── {SliceName}Decision.java       ← FUNCTIONAL CORE: State + evolve + decide, pure
src/test/java/.../slices/{context}/{slicename}/
├── {SliceName}CommandHandlerTest.java ← shell wiring: boundary, tags, clock, append, retry
└── funcore/
    └── {SliceName}Specification.java  ← the board's GWTs + storyline beats, against the pure core
```

- **Every trigger enters the shell**, never the core: REST controller, automation processor,
  listener — all call `{SliceName}CommandHandler.handle(command)`.
- **The shell allocates ALL of the core's input** (folded state, current time, generated ids,
  lookups), does ALL I/O, and appends the core's outcome.
- **The whole read → decide → append runs inside `ConflictRetry.onConflict(...)`** — 5 immediate
  attempts, re-run from scratch on a concurrent conflict.
- **The core (`funcore/`) is pure**: no `io.umadb`, no Spring, no Jakarta, no Jackson, no
  `eventstore`/`config` — not even transitively. `FunctionalCoreTest` fails the build otherwise.

UmaDB has no modelling layer of its own (no `@Command`/`@Aggregate`); every convention here — the
funcore shape, tag strings, `DecisionModelLoader.fold`/`append`, `ConflictRetry` — is this
project's own, in the shared `eventstore` package. Never call
`UmaDbClient.handle(ReadRequest/AppendRequest)` from a slice.

## Step 0: Read the slice definition

Read `.build-kit/.slices/{context}/{slicename}/slice.json`. Extract, and use **only** what's there:

- `commands[].fields[]` → Command record fields, in order
- `events[].fields[]` → Event record fields, in order
- `specifications[]` (GWT scenarios) → one `{SliceName}Specification` test method each (Step 7)
- Which command field(s) have `idAttribute: true` — the consistency boundary's and the event's tag(s)
- `storylines[]` (optional) → Step 7b

Never invent a field, business rule, or event that isn't in slice.json.

## Step 0a: Determine `{basePackage}`

Every path below is rooted at `{basePackage}.slices.{context}.{slicename}`. Resolve `{basePackage}`
as documented in `.build-kit/CLAUDE.md` — never hardcode any specific package.

## Step 1: Command

Plain record in the slice package — data only, no behaviour:

```java
package {basePackage}.slices.{context}.{slicename};

public record {SliceName}Command(String idField, String field1) {}
```

**Two or more fields have `idAttribute: true`** — add a compound id record next to it and a
convenience method on the command; the shell's `consistencyBoundary(...)` takes that id:

```java
public record {SliceName}Id(String field1, String field2) {}

public record {SliceName}Command(String field1, String field2) {
    public {SliceName}Id identifier() { return new {SliceName}Id(field1, field2); }
}
```

## Step 2: Event — only if it doesn't already exist

Check `.../slices/{context}/events/` first; add to what's there rather than duplicating.

**First slice of a new context?** Create the context's events package once, exactly like
`slices/blueprint/events/`:

| File | Purpose |
|---|---|
| `{Context}Event.java` | `public sealed interface {Context}Event permits ...` |
| `EventTags.java` | tag-key constants + `tag(key, value)` → `"key:value"` |
| `{Context}Events.java` | the context's one `EventMapping<{Context}Event>`: type, tags, class per event (`MAPPING` singleton) |
| `package-info.java` | `@NamedInterface("events")` — the context's published contract (Spring Modulith) |

Each context is its own Spring Modulith module; **only its `events` package is visible to other
contexts** (`ModularityTest` checks this). Commands, shells and cores stay internal.

The event itself:

```java
package {basePackage}.slices.{context}.events;

public record {EventName}(String idField, String field1) implements {Context}Event {

    public static final String TYPE = "{Context}.{EventName}";
}
```

Then register it — the sealed interface's `permits`, and all three switches in `{Context}Events`
(the compiler flags a missing `case` in the exhaustive ones):

```java
// {Context}Event.java
public sealed interface {Context}Event permits ..., {EventName} {}

// {Context}Events.java
case {EventName} e -> {EventName}.TYPE;                                       // typeOf
case {EventName} e -> List.of(EventTags.tag(EventTags.{TAG_CONSTANT}, e.idField())); // tagsOf
case {EventName}.TYPE -> {EventName}.class;                                  // classOf
```

**Tag every event with every id it carries** — tags decide which consistency boundaries can find
it later, and `{Context}Events` guarantees an event is tagged the same way whichever slice appends it.

**Adding an event to an existing context** makes the compiler flag every exhaustive `switch` over
`{Context}Event` in the context's cores and shells. That's intended: decide per switch whether the
new event matters there — add a `case` if it does, a `default` (e.g. `default -> state;` in an
`evolve`) if it doesn't. Never silence it any other way.

## Step 3: Functional core — `funcore/{SliceName}Decision`

Pure functions over plain values. `public` (the shell lives in the parent package).

**Derive the `State` fields from this slice's `specifications[]`, not from the event's shape.**
Each GWT's given/then pair names one decision and the prior fact it depends on — that fact is a
field. "given nothing" / "given already {X}" → one boolean for {X}; a scenario that discriminates on
a value → a field holding that value.

```java
package {basePackage}.slices.{context}.{slicename}.funcore;

import {basePackage}.slices.CommandRejectedException;
import {basePackage}.slices.{context}.events.{Context}Event;
import {basePackage}.slices.{context}.events.{EventName};
import {basePackage}.slices.{context}.{slicename}.{SliceName}Command;

import java.time.Instant;
import java.util.List;

/**
 * <b>Functional core</b> of the {SliceName} slice: pure functions over plain values - no I/O,
 * no Spring, no {@code io.umadb.client} types. Everything it needs is handed in by the imperative
 * shell ({@code {SliceName}CommandHandler}).
 */
public final class {SliceName}Decision {

    /** The facts this decision branches on - and only those. */
    public record State(boolean <ruleFlag>) {
    }

    public static final State INITIAL = new State(false);

    private {SliceName}Decision() {
    }

    public static State evolve(State state, {Context}Event event) {
        return switch (event) {
            case {EventName} e -> new State(true);
            default -> state;
        };
    }

    public static List<{Context}Event> decide(State state, {SliceName}Command command, Instant now) {
        if (state.<ruleFlag>()) {
            throw new CommandRejectedException("...");  // rule text from slice.json
        }
        return List.of(new {EventName}(command.idField(), command.field1()));
    }
}
```

`decide` has exactly three outcomes — map each GWT `then` to one of them:

| GWT `then` | `decide` returns |
|---|---|
| event(s) | `List.of(new Event(...), ...)` |
| an error / rejection | `throw new CommandRejectedException(...)` |
| nothing (a repeat is a harmless no-op) | `List.of()` |

Prefer `List.of()` over a rejection when a spec says a repeated command changes nothing — an
automation replaying from position 0 on every start then stays silent instead of logging errors.
See `ActivateItemDecision`.

**Anything not derivable from state + command is a `decide` parameter** — the current time
(`Instant now`), a generated id, a looked-up value. The core never calls `Instant.now()`,
`UUID.randomUUID()`, or anything else with I/O. Drop the `now` parameter if nothing needs it.

## Step 4: Imperative shell — `{SliceName}CommandHandler`

```java
package {basePackage}.slices.{context}.{slicename};

import {basePackage}.eventstore.ConflictRetry;
import {basePackage}.eventstore.DecisionModelLoader;
import {basePackage}.slices.{context}.events.{Context}Events;
import {basePackage}.slices.{context}.events.EventTags;
import {basePackage}.slices.{context}.events.{EventName};
import {basePackage}.slices.{context}.{slicename}.funcore.{SliceName}Decision;
import io.umadb.client.Query;
import io.umadb.client.QueryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * <b>Imperative shell</b> of the {SliceName} slice - the one entry into it for every trigger.
 * All I/O of the slice happens here and only here; the decision is delegated to the pure
 * {@link {SliceName}Decision}. The whole attempt is re-run on a concurrent conflict.
 */
@Component
@ConditionalOnProperty(prefix = "slices.{context}.write", name = "{slicename}.enabled")
public class {SliceName}CommandHandler {

    private final DecisionModelLoader loader;
    private final Clock clock;

    public {SliceName}CommandHandler(DecisionModelLoader loader, Clock clock) {
        this.loader = loader;
        this.clock = clock;
    }

    public void handle({SliceName}Command command) {
        ConflictRetry.onConflict(() -> {
            // 1. allocate the functional core's input (I/O)
            var boundary = consistencyBoundary(command.idField());
            var folded = loader.fold(boundary, {Context}Events.MAPPING, {SliceName}Decision.INITIAL, {SliceName}Decision::evolve);
            var now = clock.instant();

            // 2. functional core (pure)
            var events = {SliceName}Decision.decide(folded.state(), command, now);

            // 3. append the outcome (I/O), guarded by the same boundary it was decided on
            loader.append(events, {Context}Events.MAPPING, boundary, folded.lastPosition());
        });
    }

    /** The facts {@link {SliceName}Decision} needs. */
    static Query consistencyBoundary(String idField) {
        return Query.of(QueryItem.of(
                List.of({EventName}.TYPE),
                List.of(EventTags.tag(EventTags.{TAG_CONSTANT}, idField))
        ));
    }
}
```

Keep the three numbered comments — they are what makes the shell's structure visible at a glance.
The `Clock` bean comes from `config/ClockConfig`; drop it if `decide` needs no time.

**Retry rules** (`ConflictRetry` enforces the first two):
- Only `OptimisticConcurrencyException` is retried; a `CommandRejectedException` never is.
- The retry re-runs read → decide → append; retrying only the append would fail forever.
- Everything in the attempt runs again per retry — so only repeatable I/O belongs here (reads, the
  conditional append). Mails, payments, calls to other systems belong in an automation reacting
  to the appended event, never in a write slice's shell.

**Consistency boundary: tag each event type by what THIS decision checks — not uniformly.** Several
event types feeding one decision → several `QueryItem`s (OR'd), each with its own tag set.
Example (a `SubscribeToCourse` slice, compound id `SubscriptionId(email, courseId)`):

```java
static Query consistencyBoundary(SubscriptionId id) {
    return Query.of(List.of(
        // "is this customer registered at all" — scoped to email only
        QueryItem.of(List.of(CustomerRegistered.TYPE), List.of(EventTags.tag(EventTags.EMAIL, id.email()))),
        // "already subscribed to THIS course" — scoped to email + courseId
        QueryItem.of(List.of(SubscribedToCourse.TYPE), List.of(
                EventTags.tag(EventTags.EMAIL, id.email()),
                EventTags.tag(EventTags.COURSE_ID, id.courseId())))
    ));
}
```

Too wide silently pulls in unrelated events; too narrow silently drops events the rule needed. The
same query is passed to `fold` and `append` — never a different one. See
[references/umadb-query-patterns.md](references/umadb-query-patterns.md).

## Step 5: REST trigger — only if slice.json shows an inbound `SCREEN` dependency on the command

A trigger translates, calls the shell, translates back — nothing else:

```java
package {basePackage}.slices.{context}.{slicename};

import {basePackage}.eventstore.OptimisticConcurrencyException;
import {basePackage}.slices.CommandRejectedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "slices.{context}.write", name = "{slicename}.enabled")
public class {SliceName}RestController {

    private final {SliceName}CommandHandler commandHandler;

    public {SliceName}RestController({SliceName}CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping("/api/{context}/{resource}")
    public ResponseEntity<Void> handle(@RequestBody {SliceName}RequestBody body) {
        try {
            commandHandler.handle(new {SliceName}Command(body.idField(), body.field1()));
            return ResponseEntity.ok().build();
        } catch (CommandRejectedException e) {
            // business rule violated by the functional core
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        } catch (OptimisticConcurrencyException e) {
            // still conflicting after all retries of the imperative shell
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    public record {SliceName}RequestBody(String idField, String field1) {}
}
```

If the only inbound dependency is an `AUTOMATION`, skip this step — the automation's processor
calls the shell directly (see `build-automation`). Plain Spring MVC, not WebFlux: `UmaDbClient` is
blocking, so `ResponseEntity<...>`, not `Mono<...>`.

## Step 6: Feature flag

`@ConditionalOnProperty(prefix = "slices.{context}.write", name = "{slicename}.enabled")` on the
command handler and REST controller — never on anything in `funcore/`. Wire it in both places:

- `src/main/resources/application.properties` — `slices.{context}.write.{slicename}.enabled=true`
- `src/test/resources/application.properties` — `slices.{context}.write.{slicename}.enabled=false`

See [references/feature-flag-patterns.md](references/feature-flag-patterns.md).

## Step 7: Specification — `funcore/{SliceName}Specification` (the board's GWTs)

**One test method per `specifications[]` entry**, `@DisplayName` = the spec's title, written with
the shared `DecisionSpecification` DSL (`src/test/java/<basePackage>/testsupport/spec/`) straight
against the pure core — no client, no Spring, no mocks. Use the spec's example values.

```java
package {basePackage}.slices.{context}.{slicename}.funcore;

import {basePackage}.slices.{context}.events.{Context}Event;
import {basePackage}.slices.{context}.events.{EventName};
import {basePackage}.slices.{context}.{slicename}.{SliceName}Command;
import {basePackage}.testsupport.spec.DecisionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/** {SliceName} - the board's specifications, one test each, run against the pure core. */
class {SliceName}Specification {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private static final DecisionSpecification<{SliceName}Decision.State, {SliceName}Command, {Context}Event> SPEC =
            DecisionSpecification.of({SliceName}Decision.INITIAL, {SliceName}Decision::evolve,
                    (state, command) -> {SliceName}Decision.decide(state, command, NOW));

    @Test
    @DisplayName("{spec title from slice.json}")
    void happyPath() {
        SPEC.given()
                .when(new {SliceName}Command("id-1", "value1"))
                .then(new {EventName}("id-1", "value1"));
    }

    @Test
    @DisplayName("{spec title from slice.json}")
    void ruleViolation() {
        SPEC.given(new {EventName}("id-1", "value1"))
                .when(new {SliceName}Command("id-1", "value2"))
                .thenRejected();
    }
}
```

| GWT element | DSL |
|---|---|
| Given NOTHING | `SPEC.given()` |
| Given event(s) | `SPEC.given(event1, event2)` — folded with the core's `evolve`, in order |
| When command | `.when(command)` |
| Then event(s) | `.then(event1, ...)` — exact events, exact order |
| Then error / rejection | `.thenRejected()` |
| Then nothing / no event | `.thenNothing()` |

Given events that other slices append are used as plain records here — no need to run their
command handlers: the core only sees events.

## Step 7b: Storyline beats (optional, only if `storylines[]` is present)

A storyline usually crosses several slices, so its beats live **once per storyline** in the test
tree at `src/test/java/<basePackage>/slices/{context}/{StorylineTitle}Storyline.java` — create it
if missing, otherwise add only the beats that aren't there yet. Beats become numbered `public
static final` constants built from each beat's `fields`/`examples` (see
`blueprint/ItemLifecycleStoryline`):

```java
/** 1. COMMAND - {beat narration} */
public static final {SliceName}Command {BEAT_NAME} = new {SliceName}Command(...);
/** 2. EVENT */
public static final {EventName} {BEAT_NAME} = new {EventName}(...);
```

Then, in `{SliceName}Specification`, add one `@Nested` class per storyline (named after it) with a
test for every `COMMAND` beat of this slice's command: given = the storyline's preceding `EVENT`
beats, when = the beat, then = the `EVENT` beat(s) directly after it. If the beat after the command
isn't an `EVENT`, don't force a test. Reference beats qualified (`{StorylineTitle}Storyline.BEAT`),
never via a wildcard static import — a slice usually appears in several storylines, and they define
the same names.

```java
@Nested
@DisplayName("Storyline: " + {StorylineTitle}Storyline.TITLE)
class {StorylineTitle} {

    private static final DecisionSpecification<{SliceName}Decision.State, {SliceName}Command, {Context}Event> SPEC =
            DecisionSpecification.of({SliceName}Decision.INITIAL, {SliceName}Decision::evolve,
                    (state, command) -> {SliceName}Decision.decide(state, command, {StorylineTitle}Storyline.{TIME}));

    @Test
    @DisplayName("1 → 2: {SliceName} results in {EventName}")
    void beat() {
        SPEC.given(/* preceding EVENT beats, qualified */)
                .when({StorylineTitle}Storyline.{COMMAND_BEAT})
                .then({StorylineTitle}Storyline.{EVENT_BEAT});
    }
}
```

## Step 8: Shell test — `{SliceName}CommandHandlerTest`

The rules are covered by Step 7; this test only proves the I/O wiring, on `InMemoryUmaDbClient`
(`src/test/java/.../testsupport/`) with a fixed `Clock`. Copy `RegisterItemCommandHandlerTest` and
adapt — at minimum:

1. **Appends the decided event, tagged** — handle a command, read the store back
   (`client.handle(ReadRequest.of(Query.empty()))`), assert type, tags and
   `{Context}Events.MAPPING.decode(...)` equals the expected record.
2. **A rejection propagates** — `CommandRejectedException` reaches the caller.
3. **A concurrent write between read and append makes it re-read and re-decide** — use the
   `ConcurrentWriterClient` pattern from `RegisterItemCommandHandlerTest` (appends a conflicting
   event right before the first append); assert the outcome of the second decision and that the
   store was read twice.

If `decide` can return `List.of()`, also assert a repeat appends nothing (see
`ActivateItemCommandHandlerTest`). The retry limit itself is covered once, by `ConflictRetryTest`.

## Final Verification

- [ ] Every field in slice.json's `commands[]` is in the Command record — none invented, none missing
- [ ] Every field in slice.json's `events[]` is in the Event record — none invented, none missing
- [ ] New event registered in the sealed interface's `permits` and in all three `{Context}Events` switches
- [ ] Every `specifications[]` scenario has exactly one test method in `funcore/{SliceName}Specification`
- [ ] If `storylines[]` is present: every COMMAND beat of this slice has a `@Nested` storyline test — or was deliberately skipped as untraceable
- [ ] No business rule exists that isn't traceable to slice.json's `description`/`comments`
- [ ] `funcore/` holds only pure code; the shell does all I/O inside `ConflictRetry.onConflict`
- [ ] `fold` and `append` get the SAME boundary query
- [ ] `./mvnw compile -q`, then the slice's tests **and** `FunctionalCoreTest` + `ModularityTest`
- [ ] If checks pass, commit with `feat: {Slice Name}` and set slice status to `Done`
