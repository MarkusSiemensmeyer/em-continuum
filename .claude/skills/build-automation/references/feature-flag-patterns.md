# Feature Flag Patterns

This project's one supported approach: `@ConditionalOnProperty` on the imperative shell and its
triggers - command handler, REST controller, automation processor - and **never** on anything in a
slice's `funcore/` package, which is pure and not a Spring bean at all (see `slices/blueprint/`).
Read slices (`build-state-view`) are never feature-flagged - only write and automation slices are.
Examples use a generic `Ordering` bounded context.

## Annotation on slice components

```java
// Command handler
@ConditionalOnProperty(prefix = "slices.ordering.write", name = "placeorder.enabled")
@Component
public class PlaceOrderCommandHandler { ... }

// REST controller (if applicable)
@ConditionalOnProperty(prefix = "slices.ordering.write", name = "placeorder.enabled")
@RestController
public class PlaceOrderRestController { ... }
```

## `application.properties` (main — enable by default)

```properties
slices.ordering.write.placeorder.enabled=true
slices.ordering.automation.notifycustomeronorder.enabled=true
```

## `application.properties` (test — disable by default)

```properties
slices.ordering.write.placeorder.enabled=false
slices.ordering.automation.notifycustomeronorder.enabled=false
```

This only matters for tests that boot a Spring context (`@SpringBootTest`). The
`InMemoryUmaDbClient` unit-test pattern in
[umadb-query-patterns.md](umadb-query-patterns.md) never boots Spring, so these properties don't
affect it either way - a Testcontainers-based end-to-end test (see `build-state-view`'s reference)
overrides them back to `true` for itself via `@DynamicPropertySource`, since it's the one test that
does need the real beans wired.
