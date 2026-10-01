---
name: build-automation
authors:
  - Martin Dilger
description: >
  Implement automation slices (Event → Command) in this project's one established pattern,
  Functional Core / Imperative Shell: pure funcore Policy (react: trigger event → list of commands)
  → @Component Processor as imperative shell (SliceEventListener off the shared EventDispatcher:
  decode, ask the policy, dispatch into the target slice's CommandHandler) →
  funcore/{AutomationName}Specification (one test per board GWT, against the pure policy) + a shell
  test wired to the real target shells on InMemoryUmaDbClient. Automations can be stateless or keep
  a private read model. Also covers TRANSLATION slices - inbound (external message → our command)
  and outbound (our event → external system, recorded as a fact). Use when implementing a new
  automation / event-to-command reactor or a translation from a slice.json event model in this project. There is exactly one supported style — do not offer
  alternatives.
---

# UmaDB — Automation Slice (Functional Core / Imperative Shell)

An automation reacts to an event by dispatching a command — the **orange** stripe in Event
Modeling. Grounded in two verified references, compiled and passing under `mvn test` including
the architecture tests:

- **stateless** — `slices/blueprint/automation/activateregistereditem/` (ItemRegistered → ActivateItem)
- **with a private read model ("todo list")** — `slices/blueprint/automation/activateitemsatopenedlocation/`
  (ItemRegistered/ItemActivated keep the list, LocationOpened → ActivateItem per pending item)
 **When in doubt, open the blueprint file of the same name and copy its shape.**

## The shape — read this first

```
.../slices/{context}/automation/{slicename}/
├── {AutomationName}Processor.java    ← IMPERATIVE SHELL: decode, dispatch (SliceEventListener)
└── funcore/
    └── {AutomationName}Policy.java   ← FUNCTIONAL CORE: react(trigger) → List<Command>, pure
src/test/java/.../slices/{context}/automation/{slicename}/
├── {AutomationName}ProcessorTest.java     ← shell wired to the REAL target shell(s), in-memory store
└── funcore/
    └── {AutomationName}Specification.java ← the board's GWTs + storyline beats, against the policy
```

- **The policy decides WHETHER and WHICH commands** — pure, returns a `List` (empty = nothing).
- **The processor does the I/O**: decodes the raw event, calls the policy, dispatches each command
  into the target slice's **imperative shell** (`{TargetCommand}CommandHandler.handle`).
- **No retry in the processor** — the target shell already retries its own conflicts.
- **No idempotency guard in the processor** — `EventDispatcher` replays from position 0 on every
  start; the target slice's core must turn a repeated command into `List.of()` (see
  `ActivateItemDecision`). If it doesn't, fix the target's core rather than guarding here.
- **The policy (`funcore/`) is pure** — `FunctionalCoreTest` fails the build otherwise.

There is no command bus: the processor calls the target command handler directly — it's a Spring
bean, in-process either way.

## Step 0: Discover Target Project Conventions

> **Comments & description**: each element carries a `comments: string[]` array (board comments)
> and a `description` field — use them as implementation hints, and resolve consumed comments via
> `POST <BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/nodes/<nodeId>/comments/<commentId>/resolve`.

Read the target project's `.build-kit/CLAUDE.md` and the blueprint automation. Every path below is
rooted at `{basePackage}.slices.{context}.automation.{slicename}`; resolve `{basePackage}` as
documented in `.build-kit/CLAUDE.md`.

## Step 1: Understand the Input

| Element | What to extract from slice.json |
|---|---|
| **Trigger event** | which event triggers the automation, and which condition filters it |
| **Target command** | which command to dispatch, with what properties |
| **Mapping logic** | how event properties map to command properties |
| **Read model needed?** | does the automation need data NOT in the trigger event itself? |

GWT format for automations: `Given (events) → Then (command | NOTHING)` — setup events first,
trigger last. `storylines[]` (optional) → Step 6b.

**If requirements are unclear, invoke `/request-feedback` rather than guessing** — see
`.build-kit/CLAUDE.md`'s escalation rule.

## Step 2: Ensure Events and the Target Slice Exist

The trigger event(s) and the target command's whole slice must exist — create them first with
`build-state-change` if not (a command never exists without its slice).

**Trigger event from another context?** Import it only from that context's `events` package — its
Spring Modulith named interface. Anything else of another context is internal, and `ModularityTest`
fails the build. Dispatching into another context's command handler is not allowed either: react
in your own context with your own command instead.

## Step 3: Functional core — `funcore/{AutomationName}Policy`

```java
package {basePackage}.slices.{context}.automation.{slicename}.funcore;

import {basePackage}.slices.{context}.events.{TriggerEvent};
import {basePackage}.slices.{context}.{targetslicename}.{TargetCommand}Command;

import java.util.List;

/**
 * <b>Functional core</b> of the {AutomationName} automation: pure event-to-command mapping.
 * Decides WHETHER to react and WHICH commands to send - an empty list means "nothing". Dispatching
 * them is the shell's job ({@code {AutomationName}Processor}).
 */
public final class {AutomationName}Policy {

    private {AutomationName}Policy() {
    }

    public static List<{TargetCommand}Command> react({TriggerEvent} event) {
        if (!<condition from slice.json>) {
            return List.of();
        }
        return List.of(new {TargetCommand}Command(event.field1() /*, mapped fields */));
    }
}
```

No condition in slice.json → no `if`; never invent one.

## Step 4: Imperative shell — `{AutomationName}Processor`

```java
package {basePackage}.slices.{context}.automation.{slicename};

import {basePackage}.eventstore.SliceEventListener;
import {basePackage}.slices.{context}.automation.{slicename}.funcore.{AutomationName}Policy;
import {basePackage}.slices.{context}.events.{Context}Events;
import {basePackage}.slices.{context}.events.{TriggerEvent};
import {basePackage}.slices.{context}.{targetslicename}.{TargetCommand}CommandHandler;
import io.umadb.client.Event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * <b>Imperative shell</b> of the {AutomationName} automation: decodes the raw trigger event (I/O),
 * asks the pure {@link {AutomationName}Policy} what to do, and dispatches the resulting commands
 * into the target slice's own imperative shell - which retries its own conflicts.
 */
@Component
@ConditionalOnProperty(prefix = "slices.{context}.automation", name = "{slicename}.enabled")
public class {AutomationName}Processor implements SliceEventListener {

    private final {TargetCommand}CommandHandler {targetCommand};

    public {AutomationName}Processor({TargetCommand}CommandHandler {targetCommand}) {
        this.{targetCommand} = {targetCommand};
    }

    @Override
    public boolean supports(String eventType) {
        return {TriggerEvent}.TYPE.equals(eventType);
    }

    @Override
    public void onEvent(Event event) {
        // 1. allocate the functional core's input (I/O)
        var trigger = ({TriggerEvent}) {Context}Events.MAPPING.decode(event);

        // 2. functional core (pure)
        var commands = {AutomationName}Policy.react(trigger);

        // 3. dispatch the outcome into the target slice's imperative shell
        commands.forEach({targetCommand}::handle);
    }
}
```

`EventDispatcher` calls `supports`/`onEvent` sequentially on its one subscription thread. A
`CommandRejectedException` from the target shell is logged by the dispatcher and the next event
continues.

### With a private read model ("todo list")

When the policy needs data NOT in the trigger event, the read model becomes part of the core: an
immutable record plus **two** pure functions over the context's event type — `evolve` (setup
events change the model) and `react` (the trigger reads it). Verified shape,
`ActivateItemsAtOpenedLocationPolicy`:

```java
public final class {AutomationName}Policy {

    /** The private read model. Immutable - evolve returns a new one. */
    public record TodoList(Map<String, String> pending) {

        public static final TodoList EMPTY = new TodoList(Map.of());

        public TodoList {
            pending = Map.copyOf(pending);
        }

        TodoList with(String id, String value) { /* copy, put, new TodoList */ }
        TodoList without(String id) { /* copy, remove, new TodoList */ }
    }

    public static TodoList evolve(TodoList todoList, {Context}Event event) {
        return switch (event) {
            case {SetupEvent} e -> todoList.with(e.entityId(), e.filterField());
            case {DoneEvent} e -> todoList.without(e.entityId());      // work that's done leaves the list
            default -> todoList;
        };
    }

    public static List<{TargetCommand}Command> react(TodoList todoList, {Context}Event event) {
        return switch (event) {
            case {TriggerEvent} e -> todoList.pending().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(e.filterValue()))
                    .map(Map.Entry::getKey)
                    .sorted()                                           // deterministic order
                    .map({TargetCommand}Command::new)
                    .toList();
            default -> List.of();
        };
    }
}
```

The processor holds the current model and, per event, first reacts, then folds:

```java
private volatile TodoList todoList = TodoList.EMPTY;

@Override
public boolean supports(String eventType) {       // setup, done AND trigger types
    return {SetupEvent}.TYPE.equals(eventType) || {DoneEvent}.TYPE.equals(eventType) || {TriggerEvent}.TYPE.equals(eventType);
}

@Override
public void onEvent(Event event) {
    // 1. allocate the functional core's input (I/O) - the todo list is this shell's own state
    var domainEvent = {Context}Events.MAPPING.decode(event);

    // 2. functional core (pure): react to the event as a trigger, fold it in as a fact
    var commands = {AutomationName}Policy.react(todoList, domainEvent);
    todoList = {AutomationName}Policy.evolve(todoList, domainEvent);

    // 3. dispatch the outcome into the target slice's imperative shell
    commands.forEach({targetCommand}::handle);
}
```

- The model is **private** to this automation — never reuse another slice's read model.
- It lives **in memory only**: the dispatcher replays from position 0 on every start and rebuilds
  it. Commands re-dispatched during that replay must be no-ops in the target slice's core.
- Remove finished work from the list (a "done" event) whenever slice.json models one — otherwise
  the list only grows, and replays re-dispatch more and more commands.

## Step 5: Feature flag

`@ConditionalOnProperty(prefix = "slices.{context}.automation", name = "{slicename}.enabled")` on the
processor only — never on `funcore/`. Wire `...automation.{slicename}.enabled=true` in
`src/main/resources/application.properties` and `=false` in `src/test/resources/application.properties`.
See [references/feature-flag-patterns.md](references/feature-flag-patterns.md).

## Step 6: Specification — `funcore/{AutomationName}Specification` (the board's GWTs)

**One test method per `specifications[]` entry**, `@DisplayName` = the spec's title, written with
the shared `AutomationSpecification` DSL (`testsupport/spec/`) against the pure policy:

```java
package {basePackage}.slices.{context}.automation.{slicename}.funcore;

import {basePackage}.slices.{context}.events.{TriggerEvent};
import {basePackage}.slices.{context}.{targetslicename}.{TargetCommand}Command;
import {basePackage}.testsupport.spec.AutomationSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {AutomationName} - the board's specifications, one test each, run against the pure policy. */
class {AutomationName}Specification {

    private static final AutomationSpecification<{TriggerEvent}, {TargetCommand}Command> SPEC =
            AutomationSpecification.of({AutomationName}Policy::react);

    @Test
    @DisplayName("{spec title from slice.json}")
    void conditionMet() {
        SPEC.given(new {TriggerEvent}("entity-1" /*, fields that meet the condition */))
                .then(new {TargetCommand}Command("entity-1"));
    }

    @Test
    @DisplayName("{spec title from slice.json}")
    void conditionNotMet() {
        SPEC.given(new {TriggerEvent}("entity-1" /*, fields that do NOT meet it */))
                .thenNothing();
    }
}
```

| GWT element | DSL |
|---|---|
| Given trigger event | `SPEC.given(triggerEvent)` |
| Then command(s) | `.then(command1, ...)` — exact commands, exact order |
| Then NOTHING | `.thenNothing()` |

**With a private read model** use `ReadModelAutomationSpecification` — exactly the board's
format: setup events first, **the last given event is the trigger**:

```java
private static final ReadModelAutomationSpecification<TodoList, {Context}Event, {TargetCommand}Command> SPEC =
        ReadModelAutomationSpecification.of(TodoList.EMPTY, {AutomationName}Policy::evolve, {AutomationName}Policy::react);

@Test
@DisplayName("{spec title from slice.json}")
void matchingEntriesOnly() {
    SPEC.given(
                    new {SetupEvent}("entity-2", "filter-A"),
                    new {SetupEvent}("entity-3", "filter-B"),
                    new {TriggerEvent}("filter-A"))                 // trigger last
            .then(new {TargetCommand}Command("entity-2"));
}
```

Cover at least (see `ActivateItemsAtOpenedLocationSpecification`):
1. matching filter → commands for matching entries only
2. no matching entry → `.thenNothing()`
3. finished work (a "done" event before the trigger) → not dispatched again
4. setup after the trigger doesn't count — `given(trigger, setup)`: the last event is then a setup
   event, and `react` returns nothing for it

## Step 6b: Storyline beats (optional, only if `storylines[]` is present)

Beats live once per storyline in `src/test/java/<basePackage>/slices/{context}/{StorylineTitle}Storyline.java`
(create or extend it — see `build-state-change` Step 7b and `blueprint/ItemLifecycleStoryline`).
For every `EVENT` beat directly followed by a `COMMAND` beat this automation dispatches, add a test
to a `@Nested @DisplayName("Storyline: " + {StorylineTitle}Storyline.TITLE) class {StorylineTitle}` in the
specification — one nested class per storyline, beats referenced qualified (`{StorylineTitle}Storyline.BEAT`),
never via a wildcard static import (several storylines define the same names):

```java
@Test
@DisplayName("2 → 4: {TriggerEvent} triggers {TargetCommand}")
void beat() {
    SPEC.given({StorylineTitle}Storyline.{EVENT_BEAT}).then({StorylineTitle}Storyline.{COMMAND_BEAT});
}
```

With a private read model, the given beats are the storyline's setup EVENT beats plus the trigger
beat, trigger last (see `ActivateItemsAtOpenedLocationSpecification`). If the beat after the trigger
isn't a COMMAND this automation dispatches, don't force a test.

## Step 7: Shell test — `{AutomationName}ProcessorTest`

No mocks: wire the processor to the **real** target command handler(s) on one shared
`InMemoryUmaDbClient`, and feed it raw events exactly as `EventDispatcher` would
(`{Context}Events.MAPPING.encode(...)`, or the last event read back from the store). Copy
`ActivateRegisteredItemProcessorTest` (stateless) or `ActivateItemsAtOpenedLocationProcessorTest`
(read model — its `dispatch()` helper plays `EventDispatcher`, delivering every event in store
order, including those the automation's own commands append) and adapt — at minimum:

1. `supports` answers true for the trigger (and setup) types only.
2. Condition met → the target slice's event is in the store.
3. Condition not met → nothing appended.
4. The same trigger delivered twice (a dispatcher replay) → the target event exists only once. With
   a read model: a fresh processor fed the whole store again (a restart) → still only once.

If test 4 fails, the target slice's core must return `List.of()` for a repeat — fix it there.

## Translation slices (`sliceType === "TRANSLATION"`)

A translation connects an **external system** with this context — the same core/shell split, under
`.../slices/{context}/translation/{slicename}/`. Read `description`/`notes` in slice.json to see the
direction. Verified references: `slices/blueprint/translation/facilitysitestatus/` (inbound) and
`slices/blueprint/translation/reportitemtoassetregistry/` (outbound), both replayed end to end by
the `SiteGoesLiveStoryline`.

### Inbound: external message → our command (anti-corruption layer)

```
translation/{slicename}/
├── {ExternalMessage}.java          ← THEIR vocabulary, plain record, no annotations (core input)
├── {SliceName}Translator.java      ← IMPERATIVE SHELL: translate, dispatch into the target shell
├── {SliceName}Webhook.java         ← trigger (REST); a Kafka/Spring listener would call the same shell
└── funcore/{SliceName}Translation.java  ← FUNCTIONAL CORE: translate(message) → List<OurCommand>
```

- The core maps their vocabulary to ours (codes, statuses, ids) and **filters** what this context
  doesn't care about (`List.of()`). Tolerate unknown values: nothing, not an exception.
- The translator is trigger-agnostic: every channel decodes the message and calls `handle(message)`.
- No retry and no dedup in the translator — the target shell retries its own conflicts, and the
  target core must make a redelivered message a no-op (the sender WILL redeliver on non-2xx).
- Specification: same shape as a stateless automation — `AutomationSpecification.of({SliceName}Translation::translate)`,
  `given(message).then(command)` / `.thenNothing()`. Shell test: translator wired to the REAL
  target shell; cover "translated", "ignored", "redelivered → only once"
  (`FacilitySiteStatusTranslatorTest`).

### Outbound: our event → external system

```
translation/{slicename}/
├── {TheirMessage}.java             ← THEIR vocabulary, plain record (core output)
├── {ExternalSystem}.java           ← port interface: the external call
├── RestClient{ExternalSystem}.java ← HTTP adapter (package-private @Component, RestClient.create(url))
├── {SliceName}Translator.java      ← IMPERATIVE SHELL: SliceEventListener
└── funcore/{SliceName}Translation.java  ← FUNCTIONAL CORE: State + evolve + translate → Optional<Report>
events/{FactRecorded}.java          ← e.g. ItemReportedToAssetRegistry - recorded after delivery
```

**The dispatcher replays from position 0 on every start — an outbound translation without a
recorded fact would call the external system again for ALL history on every restart.** So:

1. The core's `State` knows whether this event was already delivered (`evolve` over the recorded
   fact); `translate(state, event, now)` returns `Optional.empty()` if so, otherwise a `Report`
   record holding **both** their message and our fact to record.
2. The shell folds the fact's boundary, calls the core, then **delivers first, records second**:

```java
report.ifPresent(r -> {
    externalSystem.send(r.notice());                                                  // I/O: deliver
    loader.append(List.of(r.recorded()), {Context}Events.MAPPING, boundary, folded.lastPosition()); // I/O: record
});
```

- **At least once**: a crash between deliver and record re-delivers on the next start — put an
  idempotency key in their message (the entity id) and say so in its Javadoc.
- **No `ConflictRetry`**: re-running would call the external system again.
- **External system down**: the port throws, nothing is recorded, the dispatcher logs it, the next
  start's replay delivers it.
- Feature flag `slices.{context}.translation.{slicename}.enabled` — **`false` in main** until the
  external system is reachable (its URL property next to it), `false` in test.
- Specification: `TranslationSpecification` — `given(facts).when(ourEvent).then(new Report(...))` /
  `.thenNothing()` for "already delivered". Shell test with an in-memory fake of the port (no mocks),
  covering "deliver then record", "replay → delivered once", "unreachable → nothing recorded, later
  delivered" (`ReportItemToAssetRegistryTranslatorTest`).

### Storyline beats for translations

`EXTERNAL` beats become constants of their message records. An inbound translation tests
`EXTERNAL → COMMAND`, an outbound one `EVENT → EXTERNAL (+ recorded EVENT)` — see
`SiteGoesLiveStoryline` beats 3 → 4 and 7 → 8 → 9.

## Final Verification

- [ ] The trigger event matches slice.json exactly; the dispatched command is the target command in slice.json
- [ ] All fields mapped from event to command come from slice.json — no invented mappings, no invented conditions
- [ ] Every `specifications[]` scenario has exactly one test method in `funcore/{AutomationName}Specification`
- [ ] If `storylines[]` is present: every trigger-EVENT → COMMAND beat pair has a `@Nested` storyline test — or was deliberately skipped as untraceable
- [ ] `funcore/` holds only the pure policy; the processor does decoding and dispatch, with no retry of its own
- [ ] Cross-context triggers only come from the other context's `events` package
- [ ] Translations: external messages are plain records in THEIR vocabulary; outbound delivers before it records, and is feature-flagged off in main until the external system is reachable
- [ ] `./mvnw compile -q`, then the slice's tests **and** `FunctionalCoreTest` + `ModularityTest`
- [ ] If checks pass, commit with `feat: {Slice Name}` and set slice status to `Done`
