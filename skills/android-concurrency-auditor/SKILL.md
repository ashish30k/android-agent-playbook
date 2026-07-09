---
name: android-concurrency-auditor
description: Audit Kotlin coroutines, Flow, and scope usage in Android code for leaks, broken cancellation, flow misuse, and dispatcher problems. Use this skill whenever the user asks about coroutine leaks, memory leaks in ViewModels, flow collection bugs, cancellation issues, "why is this still running after the screen closed", races, duplicate network calls, stateIn/shareIn behavior, or asks to review/audit any Kotlin code that launches coroutines or exposes Flows — even if they don't say the word "audit".
---

# Android Concurrency Auditor

Systematic audit of coroutine/Flow code. Work through the five passes in order — earlier passes find the bugs that make later passes moot. Report findings as you go using the output format at the bottom.

## Pass 1 — Scope & leak audit

Find work that outlives its purpose. Grep starting points: `GlobalScope`, `CoroutineScope(`, `MainScope(`, `callbackFlow`, `awaitClose`.

- **GlobalScope / hand-rolled `CoroutineScope(...)`**: who cancels it? If nobody, it lives as long as the process. A scope created in a class needs that class to have a clear teardown that cancels it — otherwise the coroutine (and everything it captures) leaks.
- **Scope lifetime mismatch**: work tied to an object that outlives the work's usefulness (e.g., a repository-held scope running UI-driven work). Screen-relevant work should follow the caller (ViewModel) via suspend functions, not be launched into a longer-lived scope.
- **`callbackFlow` without `awaitClose`**: the listener is never unregistered — a leak AND a crash (`awaitClose` is mandatory when the callback registers anything).
- **Work that legitimately must outlive the caller** (e.g., "save on exit"): should use an injected application-level scope (`@ApplicationScope CoroutineScope` with `SupervisorJob`), not `GlobalScope`. Flag `GlobalScope` even when the intent is right.

## Pass 2 — Cancellation correctness

Cancellation is cooperative; these patterns silently break it. Grep: `runCatching`, `catch (e: Exception)`, `catch (t: Throwable)`, `NonCancellable`, `suspendCancellableCoroutine`, `while (`, `runBlocking`.

- **`runCatching` around suspend calls**: catches `CancellationException` too, so cancellation is swallowed — the coroutine keeps running after its scope died. This is the single most common serious bug in coroutine codebases. Fix: rethrow `CancellationException` (or use a `runSuspendCatching` helper), or use try/catch on specific exceptions.
- **Broad `catch (e: Exception)` in a suspend function or `launch`**: same problem as runCatching. Must rethrow if `e is CancellationException`.
- **CPU-bound loops with no suspension point**: `job.cancel()` does nothing. Need `ensureActive()` or `yield()` inside the loop.
- **`NonCancellable`**: only legitimate inside `withContext(NonCancellable)` for brief cleanup in a `finally`. Anywhere else is a red flag.
- **`runBlocking` in production code**: deadlock and ANR risk; almost always wrong outside main() and tests.

## Pass 3 — Flow hygiene

Grep: `stateIn`, `shareIn`, `SharingStarted`, `collect`, `launchIn`, `flowOn`, `MutableStateFlow`, `MutableSharedFlow`.

- **`stateIn(..., Eagerly/Lazily, ...)` in ViewModels**: upstream keeps collecting with zero subscribers (app in background). `WhileSubscribed(5_000)` is the usual right answer — 5s survives configuration change without restarting the upstream. Conversely, `WhileSubscribed(0)` restarts expensive upstreams on every rotation.
- **UI collection not lifecycle-aware**: `lifecycleScope.launch { flow.collect {} }` keeps collecting when stopped. Require `repeatOnLifecycle(STARTED)` (Views) or `collectAsStateWithLifecycle` (Compose).
- **Cold flow collected by N consumers doing expensive work N times**: needs `shareIn`, or restructure.
- **`flowOn` placement**: affects only upstream operators. `flowOn(IO)` after `map` but before `collect` does not move the collector.
- **Fast producers without `conflate`/`collectLatest`** where only the latest value matters (UI state).
- **`MutableSharedFlow` for events with default replay/buffer**: dropped or missed events depending on config; check `extraBufferCapacity`/`onBufferOverflow` deliberately.

## Pass 4 — Dispatcher & threading discipline

Grep: `Dispatchers.`, `withContext`, `@Query` (Room), `.execute()` (OkHttp).

- **Hardcoded `Dispatchers.IO/Default/Main`**: untestable — tests can't substitute a TestDispatcher. Should be constructor-injected with qualifiers.
- **Blocking calls on Main or Default**: synchronous IO, non-suspend Room queries, `.execute()` network calls. `viewModelScope.launch { }` runs on Main by default — heavy work inside freezes UI.
- **`Dispatchers.Main` vs `Main.immediate`**: redundant dispatch when already on main; matters in hot paths.

## Pass 5 — Exception topology

Grep: `SupervisorJob`, `CoroutineExceptionHandler`, `supervisorScope`, `coroutineScope {`, `async`.

- **`launch` with no handler in a scope with no `CoroutineExceptionHandler`**: uncaught exception crashes the app. Map which launches are protected and which aren't.
- **A child failure cancelling siblings unexpectedly**: `coroutineScope` where `supervisorScope` was intended (or vice versa — supervisorScope hiding failures that should abort).
- **`async` whose `await` is never called or is awaited outside try/catch**: exceptions deferred and surfacing in surprising places (and with SupervisorJob, possibly never).

## Output format

For each finding:

| # | Severity | File:Line | Pattern | Why it's wrong here | Fix |
|---|----------|-----------|---------|--------------------|-----|

Severity: **P0** = leak or swallowed cancellation on a hot path / crash risk. **P1** = correctness bug needing specific timing. **P2** = untestable/fragile but currently working.

After the table: the single highest-leverage structural change (not per-line fixes), and which findings a lint rule could prevent permanently (suggest the rule).

Verify before reporting: for each P0, trace the actual leak path or cancellation scenario — object A holds B until event C. If you can't complete the trace, downgrade it and say why.
