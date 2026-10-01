# Project Configuration

Read events in `src/main/java/<basePackage>/slices/{context}/events/` to understand the global
structure - one sealed event interface plus an `EventTags` class per context.

`<basePackage>` in this project's Java code (`src/main/java/<basePackage>/slices/...`) is this
project's own Java package prefix, not a fixed value - resolve it, in order: (1) the package of the
project's `@SpringBootApplication` class, (2) the package of any existing slice already under
`.../slices/{context}/{slicename}/`, (3) only if no code exists yet, Maven's `<groupId>` in
`pom.xml`. Never hardcode `io.umadb.quickstart` (the shipped quickstart scaffold's package) or any
other specific package.

## File Structure Constraints

- **Strict Path Limitation**: if not instructed otherwise, only check
  `src/main/java/<basePackage>/slices/{context}/{slicename}/**/*.java` (the slice and its `funcore/`)
  and its test counterpart under `src/test/java/...`, plus the context's `events/` package
- **Slice Organization**: each feature/domain is a separate slice, flat under its context - no
  `write`/`read`/`automation` folder layer in between (only the shared `slices/{context}/events/`
  folder sits alongside slice folders, and automations live under `slices/{context}/automation/{slicename}/`, translations under
  `slices/{context}/translation/{slicename}/`)

## Code Standards

- **Language**: Java 27 (`java.version` in `pom.xml`)
- **Module System**: standard Maven `src/main/java` / `src/test/java` layout
- **Framework**: Spring Boot (plain MVC, not WebFlux - UmaDB's client API is blocking)
- **Type Safety**: commands, events, query/result types, core states and read-model rows are
  records; functional cores are `final` classes of `public static` pure functions
- **Functional Core / Imperative Shell**: every slice splits into a pure `funcore/` sub-package
  (decision, policy or projection) and an imperative shell around it (command handler, processor,
  projector) that does all I/O. Write shells wrap read → decide → append in `ConflictRetry`.
  `FunctionalCoreTest` fails the build if `funcore/` touches UmaDB, Spring, Jakarta, Jackson or the
  `eventstore`/`config` packages - even transitively
- **Modules**: each `slices/{context}` is a Spring Modulith module; only its `events` package
  (`@NamedInterface("events")`) is visible to other contexts. `ModularityTest` verifies this

## Development Guidelines

1. Each slice should be self-contained and focused on a specific domain
2. Maintain clear separation of concerns within each slice
3. Reuse the shared infrastructure rather than re-implementing it per slice: `eventstore`
   (`DecisionModelLoader.fold`/`append`, `ConflictRetry`, `EventMapping`, `EventCodec`,
   `SliceEventListener`, `EventDispatcher`), `slices.CommandRejectedException`, `config.ClockConfig`,
   and in tests `testsupport/spec` (`DecisionSpecification`, `AutomationSpecification`,
   `ReadModelAutomationSpecification`, `ProjectionSpecification`, `TranslationSpecification`) and
   `testsupport/InMemoryUmaDbClient`
4. `slices/blueprint/` is the verified reference context for every slice type - copy its shapes

Ignore case for files and slices in prompts. "CartItems" slice is the same as "cartitems".

Do not change test files unless explicitly instructed, or the change brings the test in line with slice.json (e.g. step 4's field/spec diff).

At the start of every session, read `.build-kit/AGENTS.md` if it exists to load accumulated project learnings.

When starting to work on a slice, invoke the `update-slice-status` skill with `InProgress` status before doing anything else.

## Building a Slice

**CRITICAL: You MUST always use the provided skills to build slices. NEVER implement a slice manually.**
**ALL fields, event names, command names, and business rules MUST come exclusively from slice.json. Do NOT invent, assume, or guess any field or logic not present in the slice definition.**

**Default: make a reasonable assumption and build the slice.** If a detail is unclear or missing (an example value, a field type, a status mapping, a referenced event that isn't modeled yet, ...), pick the most sensible interpretation from `slice.json`, its specifications and the surrounding model, build it, and record each assumption in one line in `progress.txt` and as a code comment. Ambiguity alone is never a reason to stop. **Only if the slice literally cannot be built** — nothing runnable can be produced even with sensible assumptions — invoke `/request-feedback` with the specific question; it posts the question as a comment on the slice and marks it `Blocked`. That must be the absolute exception; a `Blocked` slice should mean "impossible without a human", never "the agent preferred to ask".

When asked to build a slice, always follow this flow:

1. Read the slice definition from `.build-kit/.slices/<context>/<slicename>/slice.json`.
2. Determine the slice type:
   - **Translation** — `sliceType === "TRANSLATION"` → read `description` and `notes` from slice.json for the direction (inbound: external message → our command; outbound: our event → external system), then invoke `/build-automation` and follow its "Translation slices" section
   - **Automation** — `processors` array is non-empty → invoke `/build-automation`
   - **State-view** — `projections` or `queries` array is non-empty → invoke `/build-state-view`
   - **State-change** — default (has `commands` / `events`) → invoke `/build-state-change`
3. Invoke the matching skill and follow its instructions completely. Do not deviate.
4. **Verify against slice.json**: After the skill completes, check that every command field, event field, and specification in slice.json appears in the implementation. No invented fields — if it is not in slice.json, it must not be in the code. This applies even when the slice was previously `Done` and reappears as `Planned` — never dismiss a mismatch as "already implemented" or harmless drift; diff slice.json against the code field by field and update the code to match every change.
5. Run quality checks (`./mvnw compile -q`, then the slice tests only).
6. If checks pass, commit with `feat: [Slice Name]` and set slice status to `Done`.

After you are done, automatically run the tests for the slice that was edited.

## Example Slice Structure

```
src/main/java/<basePackage>/slices/
├── {context}/                             ← one Spring Modulith module per context
│   ├── events/                            ← the context's published API (@NamedInterface "events")
│   │   ├── {Context}Event.java            ← sealed interface every event in this context implements
│   │   ├── {Context}Events.java           ← EventMapping: type, tags, class per event
│   │   ├── EventTags.java                 ← "key:value" tag-string constants
│   │   └── package-info.java              ← @NamedInterface("events")
│   ├── {slicename}/                       ← write slice (build-state-change)
│   │   ├── {SliceName}Command.java
│   │   ├── {SliceName}CommandHandler.java ← imperative shell (all I/O, ConflictRetry)
│   │   ├── {SliceName}RestController.java    (only if a SCREEN depends on the command)
│   │   └── funcore/{SliceName}Decision.java  ← functional core: State, evolve, decide
│   ├── {slicename}/                       ← read slice (build-state-view)
│   │   ├── Get{SliceName}.java            ← query record + nested Result
│   │   ├── {SliceName}Projector.java      ← imperative shell (SliceEventListener, JPA)
│   │   ├── {SliceName}Entity.java + {SliceName}Repository.java
│   │   ├── {SliceName}RestApi.java
│   │   └── funcore/{SliceName}Projection.java + {Row}.java  ← functional core: evolve(row, event)
│   ├── automation/{slicename}/            ← automation slice (build-automation)
│   │   ├── {AutomationName}Processor.java ← imperative shell (SliceEventListener)
│   │   └── funcore/{AutomationName}Policy.java  ← functional core: react(event) → commands
│   │                                                (+ TodoList + evolve with a private read model)
│   └── translation/{slicename}/           ← translation slice (build-automation, "Translation slices")
│       ├── {ExternalMessage}.java         ← the external system's vocabulary, plain record
│       ├── {SliceName}Translator.java     ← imperative shell (inbound: called by a webhook/listener;
│       │                                    outbound: SliceEventListener + port to the external system)
│       └── funcore/{SliceName}Translation.java  ← functional core: their message ⇄ our command/event

src/test/java/<basePackage>/slices/{context}/
├── {StorylineTitle}Storyline.java         ← a board storyline's beats, shared by the specifications
└── {slicename}/
    ├── {SliceName}CommandHandlerTest.java | {SliceName}ProjectorTest.java | {AutomationName}ProcessorTest.java
    └── funcore/{SliceName}Specification.java  ← one test per board GWT (+ @Nested storyline beats)
```
