# Concurrency, Flow & ViewModel Review Checklist

The largest crash/leak/ANR surface in the codebase. Each rule: check → why. (Codebase-wide sweeps: use the `android-concurrency-auditor` skill; this file is for reviewing diffs.)

## Scopes & structured concurrency
- No `GlobalScope`; no hand-rolled `CoroutineScope(...)` without an owner that cancels it in a teardown path. Work that must outlive the screen (e.g., "submit payment even if user leaves") goes to an injected `@ApplicationScope` (SupervisorJob) or WorkManager — WorkManager when it must survive process death, which for a bank is usually the right answer for uploads/submissions.
- `callbackFlow` must end with `awaitClose { unregister }` — missing it is both a leak and an `IllegalStateException`.
- Child coroutines launched from the right scope: UI-bound work in `viewModelScope`; nothing UI-bound launched from repository-owned scopes. *Why: repo-scoped UI work keeps running for screens that no longer exist.*

## Cancellation correctness
- `runCatching` or `catch (e: Exception)` around suspend calls must rethrow `CancellationException`. *Why: swallowed cancellation means the coroutine outlives its scope — leaked work, and in payment flows, a duplicate-operation risk.*
- CPU loops need `ensureActive()`/`yield()`. `NonCancellable` only inside `finally` cleanup.
- Cleanup that must happen on cancellation (closing resources, releasing locks) lives in `finally` or `awaitClose`, not after the suspension point.

## Flow hygiene
- `stateIn`/`shareIn` strategy deliberate: `WhileSubscribed(5_000)` is the ViewModel default; `Eagerly` on anything expensive (location, sockets, polling) is a finding. Justify any deviation in the PR.
- UI collection lifecycle-aware: `repeatOnLifecycle(STARTED)` (Views) / `collectAsStateWithLifecycle` (Compose). Plain `lifecycleScope.launch { collect }` in `onStart`/`onResume` stacks collectors across restarts.
- Cold flows doing expensive work with multiple collectors → `shareIn`. Fast producers feeding UI → `conflate`/`collectLatest`.
- One-shot events (navigation, toasts, dialogs): don't model as `StateFlow` (re-fires on rotation/re-subscribe — a re-shown "payment confirmed" dialog is a real incident). Prefer modeling as state the UI consumes-and-clears; `Channel(BUFFERED)` acceptable with documented delivery semantics.

## ViewModel rules
- Expose `StateFlow`/immutable types; `MutableStateFlow` stays private. No public suspend functions on ViewModels (UI can't scope them correctly).
- No `View`, `Fragment`, `Activity`, or their `Context` referenced in a ViewModel (Application context via DI only). *Why: ViewModel outlives them across config changes — guaranteed leak.*
- State the user can't afford to lose (mid-transfer inputs, flow progress) in `SavedStateHandle`, not just in-memory. ViewModels survive rotation, not process death.
- Heavy eager work in `init`/property initializers is a finding — ties expensive startup to ViewModel construction and breaks testability; prefer lazy/`WhileSubscribed`-driven starts.
- Updates to shared state via `MutableStateFlow.update { }`, not read-modify-write (`.value = .value.copy(...)` races under concurrent updates).

## Threading & main-safety
- Convention: every suspend function is main-safe — repositories/data sources move themselves off Main (`withContext(injectedDispatcher)`); callers never compensate with `launch(Dispatchers.IO)`. A `viewModelScope.launch(Dispatchers.IO)` is a smell that a lower layer isn't main-safe.
- Dispatchers constructor-injected with qualifiers; hardcoded `Dispatchers.*` in new code is a finding (untestable).
- No `runBlocking` outside tests/main(). No synchronous IO, non-suspend DAO calls, or `.execute()` on Main. *Why: Main blocking = ANR; ANR rate gates Play Store visibility.*
- Shared mutable state reachable from 2+ coroutines: `StateFlow.update`, `Mutex`, or confinement. `@Volatile`/`synchronized` sprinkled into coroutine code is a yellow flag — usually the state model is wrong.
- `Dispatchers.Main.immediate` vs `Main` considered on hot paths (avoid redundant dispatch).

## Testing the async code in this diff
- New ViewModel/repository logic: tests use `runTest` with injected `TestDispatcher` (via `Dispatchers.setMain` in a rule or a dispatcher seam) — real `delay`s or `Thread.sleep` in tests are findings.
- Never-completing collectors in tests belong in `backgroundScope`; Flow assertions via Turbine.
- Critical flows test cancellation (screen closed mid-operation) and error paths, not just happy path. If cancellation is hard to test, the design is wrong — say so.
