# General Review Guidelines — Architecture, Concurrency & Prod-Readiness

The standard: nothing in this diff should be capable of producing a prod incident — crash, ANR, leak, wrong number on screen, or unrecoverable state. Each rule: check → why.

## Money
- Amounts as `BigDecimal` or `Long` minor units — never `Double`/`Float`, never float literals in tests asserting amounts. *Why: 0.1 + 0.2 != 0.3; a rounding penny across millions of transactions is an incident and an audit finding.*
- Rounding mode explicit at every division/percentage (`RoundingMode.HALF_EVEN` typical for finance) — a bare `setScale` or division is a finding.
- Currency attached to the amount (Money type), not implied. Formatting locale-aware and separate from arithmetic. No `==` on floating types anywhere near money.
- Server-provided amounts echoed back for confirmation flows, not recomputed client-side.

## Concurrency, Flow, ViewModels & threading

(Codebase-wide sweeps: `android-concurrency-auditor` skill.)

### Scopes & structured concurrency
- No `GlobalScope`; no hand-rolled `CoroutineScope(...)` without an owner that cancels it in a teardown path. Work that must outlive the screen (e.g., "submit payment even if user leaves") goes to an injected `@ApplicationScope` (SupervisorJob) or WorkManager — WorkManager when it must survive process death, which for a bank is usually the right answer for uploads/submissions.
- `callbackFlow` must end with `awaitClose { unregister }` — missing it is both a leak and an `IllegalStateException`.
- Child coroutines launched from the right scope: UI-bound work in `viewModelScope`; nothing UI-bound launched from repository-owned scopes. *Why: repo-scoped UI work keeps running for screens that no longer exist.*

### Cancellation correctness
- `runCatching` or `catch (e: Exception)` around suspend calls must rethrow `CancellationException`. *Why: swallowed cancellation means the coroutine outlives its scope — leaked work, and in payment flows, a duplicate-operation risk.*
- CPU loops need `ensureActive()`/`yield()`. `NonCancellable` only inside `finally` cleanup.
- Cleanup that must happen on cancellation (closing resources, releasing locks) lives in `finally` or `awaitClose`, not after the suspension point.

### Flow hygiene
- `stateIn`/`shareIn` strategy deliberate: `WhileSubscribed(5_000)` is the ViewModel default; `Eagerly` on anything expensive (location, sockets, polling) is a finding. Justify any deviation in the PR.
- UI collection lifecycle-aware: `repeatOnLifecycle(STARTED)` (Views) / `collectAsStateWithLifecycle` (Compose). Plain `lifecycleScope.launch { collect }` in `onStart`/`onResume` stacks collectors across restarts.
- Cold flows doing expensive work with multiple collectors → `shareIn`. Fast producers feeding UI → `conflate`/`collectLatest`.
- One-shot events (navigation, toasts, dialogs): don't model as `StateFlow` (re-fires on rotation/re-subscribe — a re-shown "payment confirmed" dialog is a real incident). Prefer modeling as state the UI consumes-and-clears; `Channel(BUFFERED)` acceptable with documented delivery semantics.

### ViewModel rules
- Expose `StateFlow`/immutable types; `MutableStateFlow` stays private. No public suspend functions on ViewModels (UI can't scope them correctly).
- No `View`, `Fragment`, `Activity`, or their `Context` referenced in a ViewModel (Application context via DI only). *Why: ViewModel outlives them across config changes — guaranteed leak.*
- State the user can't afford to lose (mid-transfer inputs, flow progress) in `SavedStateHandle`, not just in-memory. ViewModels survive rotation, not process death.
- Heavy eager work in `init`/property initializers is a finding — ties expensive startup to ViewModel construction and breaks testability; prefer lazy/`WhileSubscribed`-driven starts.
- Updates to shared state via `MutableStateFlow.update { }`, not read-modify-write (`.value = .value.copy(...)` races under concurrent updates).

### Threading & main-safety
- Convention: every suspend function is main-safe — repositories/data sources move themselves off Main (`withContext(injectedDispatcher)`); callers never compensate with `launch(Dispatchers.IO)`. A `viewModelScope.launch(Dispatchers.IO)` is a smell that a lower layer isn't main-safe.
- Dispatchers constructor-injected with qualifiers; hardcoded `Dispatchers.*` in new code is a finding (untestable).
- No `runBlocking` outside tests/main(). No synchronous IO, non-suspend DAO calls, or `.execute()` on Main. *Why: Main blocking = ANR; ANR rate gates Play Store visibility.*
- Shared mutable state reachable from 2+ coroutines: `StateFlow.update`, `Mutex`, or confinement. `@Volatile`/`synchronized` sprinkled into coroutine code is a yellow flag — usually the state model is wrong.
- `Dispatchers.Main.immediate` vs `Main` considered on hot paths (avoid redundant dispatch).

### Testing the async code in this diff
- New ViewModel/repository logic: tests use `runTest` with injected `TestDispatcher` (via `Dispatchers.setMain` in a rule or a dispatcher seam) — real `delay`s or `Thread.sleep` in tests are findings.
- Never-completing collectors in tests belong in `backgroundScope`; Flow assertions via Turbine.
- Critical flows test cancellation (screen closed mid-operation) and error paths, not just happy path. If cancellation is hard to test, the design is wrong — say so.


## Lifecycle & state (crash class #1 in review)
- Survives process death: critical flow state in `SavedStateHandle`/persistence, not only in ViewModel/singletons. *Why: Android kills backgrounded banking apps constantly (they're heavy and rarely foreground); users return mid-transfer to a restored process.*
- Config change: no Activity/Fragment/View references held beyond their lifetime (in ViewModels, singletons, callbacks, coroutines). No `lateinit` that can be hit before init after restore.
- Transaction/payment flows: re-entry safe — double-tap on "Pay", back navigation mid-flow, and notification re-entry can't duplicate or corrupt the operation. Idempotency keys for anything money-moving.

## Errors & resilience
- Every network/DB call has a defined failure path that reaches the user as state, not a silent log. Sealed result types over thrown exceptions across layer boundaries.
- Retries: bounded with backoff, only on idempotent operations. *Why: a retry storm on a payment endpoint is both an outage amplifier and a duplicate-payment risk.*
- Offline: new features define behavior with no/flaky connectivity — cached view, queued action, or explicit block; never a spinner forever or a crash.
- Timeouts explicit on all remote calls; parsing tolerant of unknown enum values/fields (server evolves independently). *Why: a new server-side enum value crashing older app versions is the classic fintech prod incident.*

## Boundaries & DI (multi-module)
- Feature modules never depend on each other directly — only via api/contract modules. New cross-module imports of internal types are findings.
- Public surface minimal (explicit API mode where enabled); internal models don't leak across boundaries — map at the edge.
- DI scoping correct: nothing Activity-scoped captured in Singleton, expensive unscoped bindings, or Context leaks into long-lived graphs.
- New module follows convention plugins; no copy-pasted build logic.

## Performance
- Nothing added to startup path without justification (new initializers, eager DI, ContentProviders). Heavy work off Main; RecyclerView/LazyList item work O(small).
- New Compose state reads scoped tightly (unstable params, missing keys, wide invalidation scopes = jank findings on hot screens).
- Memory: bitmap handling, listener registration/unregistration pairing, no caches without bounds.

## Data & schema changes
- DB migrations present and tested for every schema change; destructive migration never enabled in release. *Why: losing a local transaction cache on upgrade looks like losing money to the user.*
- Serialized formats (prefs, DataStore, JSON) evolve backward-compatibly — old data readable by new code AND vice versa during staged rollout. Version fields on persisted models.

## Release safety
- Risky changes behind a real kill-switch flag (server-controlled, default-safe). Flag removal has a ticket/date. *Why: rollback via store takes days; a flag takes minutes.*
- Staged rollout compatibility: old and new app versions coexist against the same backend during rollout — check API/contract assumptions both ways.
- New metrics/alerts for new critical paths: if this feature breaks at 2am, what pages someone? If nothing, say so as a finding.
- minSdk/API-level guards for new platform APIs; native lib changes maintain 16 KB page-size alignment.

## Testing bar
- Business logic (especially money math and state machines): unit tested including edge and failure cases, not just happy path.
- Cancellation and process-death paths tested for critical flows (runTest + Turbine; SavedStateHandle restore tests).
- A bugfix without a regression test is incomplete — the test proves understanding of the bug.
