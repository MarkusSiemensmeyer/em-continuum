# UmaDB Query, AppendCondition & Test Fake — Cheat Sheet

Verified against `io.github.domenicdev:umadb-java-client:0.7` (package `io.umadb.client`) and a real
`umadb/umadb:0.7.5` server (Testcontainers), and against the `RegisterCustomer`/`SubscribeToCourse`
slices this pattern was built from.

## `Query` / `QueryItem` matching semantics

A `Query` is a list of `QueryItem`s. **Items are OR'd together** - an event matches the query if it
matches ANY item. Within one item:

- `types` (OR): event matches if `types` is empty, or the event's `type()` is one of them
- `tags` (AND): event matches if `tags` is empty, or the event's `tags()` contains ALL of them
- an item matches only if BOTH the type condition and the tag condition match

An **empty `Query`** (`Query.empty()`, or `Query.of(List.of())`) matches every event - never pass
`null`/empty expecting "match nothing".

```java
// "CustomerRegistered for this email" OR "SubscribedToCourse for this email+courseId"
Query.of(List.of(
    QueryItem.of(List.of(CustomerRegistered.TYPE), List.of(EventTags.tag(EventTags.EMAIL, email))),
    QueryItem.of(List.of(SubscribedToCourse.TYPE), List.of(
            EventTags.tag(EventTags.EMAIL, email), EventTags.tag(EventTags.COURSE_ID, courseId)))
));
```

**Tag scope is a property of the rule, not the event type.** The same event type can legitimately
need a wider or narrower tag set depending on which decision is consuming it - see
the `SubscribeToCourse` example in `build-state-change` Step 4 (the shell's `consistencyBoundary`): `CustomerRegistered` is
scoped to `email` alone (the rule is "registered at all"), but `SubscribedToCourse` is scoped to
`email` AND `courseId` together (the rule is "subscribed to THIS course", not "subscribed to any
course"). Getting this wrong doesn't fail loudly - too wide silently pulls in unrelated events, too
narrow silently drops events the rule needed.

## `AppendCondition` - the actual consistency boundary

```java
AppendCondition.failIfExistsAfter(query, lastPosition)
```

Fails the append (throws `UmaDbException.IntegrityException`) if any event matching `query` exists
at a position **strictly greater than** `lastPosition` - i.e. appeared after the position this
command's `DecisionModelLoader.fold` call observed via `getHeadPosition()`. Always pass the SAME
query used to fold the core's state - a narrower/different query here silently weakens the consistency
guarantee (a conflicting write could slip through undetected).

`DecisionModelLoader.append` already wraps this and translates the exception into
`OptimisticConcurrencyException` - don't call `AppendCondition`/`client.handle(AppendRequest...)`
directly from a command handler; use the loader - inside `ConflictRetry.onConflict(...)`, which
re-runs fold → decide → append on that exception (up to 5 immediate attempts).

**Idempotency**: appending an `Event` whose `id` (a `UUID`) already exists in the store is a no-op
that returns the existing position, regardless of any condition - verified against the client's own
`UmaDbClientTest#testIdempotentAppendReturnsSamePosition`. `Event.of(...)` generates a random id per
call, so this only matters if you're deliberately re-sending the exact same `Event` instance/id for
retry safety - not something a normal command handler needs to think about.

## Testing the shell: `InMemoryUmaDbClient`

UmaDB has no test-fixture library (unlike `axon-test`'s `AxonTestFixture`). This project's
`src/test/java/.../testsupport/InMemoryUmaDbClient` (shipped in the root scaffold) is a from-scratch
`UmaDbClient` implementation replicating the matching/condition/idempotency rules above, fast and
container-free. Shell tests use it directly:

```java
var client = new InMemoryUmaDbClient();
var handler = new RegisterItemCommandHandler(new DecisionModelLoader(client), Clock.fixed(NOW, ZoneOffset.UTC));

handler.handle(new RegisterItemCommand("item-1", "Pump", true));

var stored = client.handle(ReadRequest.of(Query.empty())).next().events();   // read the store back
```

This fake is for the **shell** tests only (wiring, tags, append, retry). The board's GWTs run
against the pure functional core via `funcore/{SliceName}Specification` and need no client at all
- see `build-state-change` Step 7.

`InMemoryUmaDbClient.subscribe(...)` deliberately throws `UnsupportedOperationException` - no
command-handler test needs it. Projectors and automation processors are tested by handing their
`onEvent(...)` a raw event directly (`{Context}Events.MAPPING.encode(...)`) - see those skills. The
one thing that genuinely needs a live subscription (`EventDispatcher` itself) belongs in a
Testcontainers test against a REAL `umadb/umadb` server. Don't try to make the
fake support subscription; use the real container for that one case.
