# Claude Code Prompts for Senior/Staff Android Platform Work

Prompts for repo-aware agents (Claude Code / IDE agent). Replace `[...]` placeholders. Verified against the mid-2026 toolchain: AGP 9.x (Gradle 9.1+, JDK 17, built-in Kotlin — no separate KGP apply), Kotlin 2.2+/K2, target SDK 36, and Google Play's 16 KB page-size requirement (mandatory for all app updates since May 2026).

**Two habits that make every prompt below better:**

1. **End with a verification demand.** "Verify by running X / show me the diff / prove it with a measurement" — agents are confident; make them earn it.
2. **Invert the interview.** For anything ambiguous, tell the agent to ask *you* questions before writing code. You have context it can't infer.

---

## 1. Understanding unfamiliar territory

### Blind spot pass
> I'm about to work on `[module/subsystem]` which I don't know well. Do a blind spot pass: scan the module and tell me my unknown unknowns — hidden invariants, non-obvious owners/consumers, gotchas in the build setup, tests that guard behavior I might break, and any tribal-knowledge patterns (naming conventions, DI scoping, threading rules) I'd violate as a newcomer. Then tell me what questions I should be asking you that I haven't.

### Codebase archaeology
> Trace the full lifecycle of `[feature/class, e.g. a push notification, a deep link, an image load]` through this codebase — from entry point to final effect. List every module boundary it crosses, every threading hop, and every place where behavior is configured (flags, remote config, build variants). Output a sequence diagram in Mermaid and a list of "surprising" findings.

### Dependency blast radius
> I'm planning to change `[API/class/module]`. Before I touch anything: find every consumer (including reflection, DI graph wiring, service loaders, and generated code), classify them by risk, and tell me which teams own them based on CODEOWNERS/git blame. Which consumers would break silently rather than at compile time?

### Architecture interview (inverted)
> Here's my rough spec: `[paste spec or link doc]`. Before proposing anything, interview me. Prioritize questions whose answers would change the architecture — scale, ownership boundaries, backward-compat constraints, rollout strategy. One question at a time. Stop when additional answers wouldn't change the design.

---

## 2. Architecture & API design (platform/SDK work)

### API review as an adversarial consumer
> Here's a public API I'm designing for other teams: `[paste interface/module]`. Role-play three consumers: (a) a junior engineer who will misuse it in the laziest way possible, (b) a team with a legacy codebase that can't adopt coroutines/Compose, (c) a team that will call it from a background process. For each: show the misuse, the failure mode, and how the API shape could prevent it. Then propose the revised API.

### Binary/source compatibility audit
> Diff the public API surface of `[module]` between `[ref A]` and `[ref B]` (use metalava/binary-compatibility-validator if configured; otherwise derive it). Classify every change as binary-breaking, source-breaking, or behavioral. For behavioral changes, find the tests that should have caught them — and flag where no test exists.

### Migration strategy with a kill-switch
> Design a migration from `[old thing]` to `[new thing]` for a codebase this size. Requirements: incremental (per-module or per-screen), each step independently shippable and revertible behind a flag, old and new paths coexist, and there's a measurable definition of "done" so the old path actually gets deleted. Output: phase plan, lint/Danger rule to prevent new usage of the old path, and the tracking mechanism.

### Design-by-prototype (react, don't spec)
> Before we commit to a design for `[component/API]`: build 3 wildly different working prototypes of the API surface (just interfaces + one fake implementation each, in a scratch package). Make them genuinely different in philosophy — e.g. callback-based vs Flow-based vs declarative config. I'll react to them rather than review a doc.

### DI / module graph critique
> Map the current DI graph (Hilt/Dagger/koin — detect which) for `[app/module]`. Identify: scope leaks, components that force unrelated features to compile together, bindings that make testing painful, and anything that blocks modularization. Rank by cost-to-fix vs payoff.

---

## 3. Build systems & modularization

### Build health audit
> Audit this project's build setup against current best practice (AGP 9.x, Gradle 9, configuration cache, built-in Kotlin support). Check: convention plugins vs copy-pasted build logic, api vs implementation dependency hygiene, configuration-cache and build-cache compatibility, unnecessary kapt (should be KSP), and module graph shape (depth, bottleneck modules everyone depends on). Produce a prioritized fix list with estimated build-time impact — and verify at least the top claim by running a build scan or `gradle --profile`.

### Module extraction with proof
> Extract `[code/feature]` from `[monolith module]` into a new module following the existing convention plugins. Rules: no circular deps, public surface as small as possible, existing tests keep passing. After the move, prove it: run the affected tests, run `./gradlew :newmodule:dependencies` and show me the module's dependency list is minimal.

### Convention plugin consolidation
> Find every piece of duplicated build logic across our `build.gradle(.kts)` files (android config blocks, compiler flags, test setup, flavor config). Design a convention-plugin structure to consolidate it, migrate two representative modules as a demo, and list the drift you found between modules (things that were supposed to be identical but aren't — those are latent bugs).

### CI failure archaeology
> Here's a flaky/failing CI build log: `[paste or point to log]`. Don't guess. Form 3 hypotheses ranked by likelihood, then tell me what evidence in the repo would confirm/refute each, gather that evidence, and only then propose the fix. If it's flakiness, find the shared state or ordering assumption.

---

## 4. Performance & app size

### Startup regression hunt
> Cold start regressed around `[date/release]`. Investigate: diff what changed in that window (deps added, initializers registered, ContentProviders, App Startup entries, DI graph growth). Check Baseline Profile coverage still matches current hot paths. Output a ranked suspect list with the evidence for each, and the cheapest measurement that would confirm the top suspect before we invest in a fix.

### App size audit
> Audit app size: run bundletool/APK analyzer on the current build, break down by dex/resources/native/assets, flag the top 10 contributors, check R8 config for over-broad keep rules, find assets that should be served from CDN instead of bundled, and audit native `.so` files (including 16 KB page-size alignment compliance). Deliver: findings table + the 3 highest-ROI reductions with estimated savings — verify estimates by actually measuring, not guessing.

### Jank forensics
> `[Screen/flow]` janks. Instrument a hypothesis-first investigation: list what typically causes jank in this kind of screen (recomposition storms, main-thread IO, sync layout passes, bitmap decodes), then inspect the actual code and rank which apply here. For Compose screens, check for unstable parameters, missing keys, and reads that invalidate wide scopes. Show me the specific lines and the fix for the top offender.

### Perf gate design
> Design a CI perf gate for `[startup/size/frame metrics]` that a 30-person team won't route around: what to measure, on what hardware/emulator config, statistical treatment (how many runs, what counts as regression vs noise), and the escape hatch policy. Generate the Gradle/CI wiring for the size gate as a starting point.

---

## 5. Staff-level leverage

### Design doc red-team
> Here's my design doc: `[paste/link]`. Red-team it as three reviewers: a skeptical principal engineer (attacks the core premise and alternatives-not-considered), an SRE (attacks rollout, monitoring, failure modes), and the engineer who has to maintain it in 2 years (attacks complexity and ownership). Give me the 5 hardest questions I'll get in review, and draft answers for the ones the doc already supports.

### RFC/PR review at depth, fast
> Review `[PR/RFC]` the way a staff engineer would in 20 minutes: skip style, focus on (1) does the approach match the stated problem, (2) irreversible decisions being made casually, (3) API/schema changes that outlive the feature, (4) what's missing — tests, migration, rollback, metrics. Format as review comments I can adapt, marked blocking vs non-blocking.

### Incident write-up from raw material
> Here's the raw material from an incident: `[logs, timeline, Slack excerpts, crash data]`. Draft a blameless postmortem: timeline, root cause vs proximate cause, why our safeguards didn't catch it, and action items that are structural (not "be more careful"). Flag where the evidence is thin and I need to verify before publishing.

### Mentoring artifact generator
> I just finished `[gnarly task]` with you. Turn the journey into a teaching artifact for mid-level engineers on my team: the decision points, what a less experienced engineer would have done at each, why, and what the better instinct is. Short — one page, no fluff.

### Delegation packet
> I need to hand `[task]` to another engineer. Write the delegation packet: context they lack, the constraint that isn't obvious, definition of done, the two rabbit holes to avoid, and which decisions they should make themselves vs escalate. Include pointers to the exact files/docs.

---

## 6. Workflow meta-prompts (use inside any task)

### Unknowns log
> While you work on this, keep a running `UNKNOWNS.md`: every assumption you made, every place you chose between plausible options, every API you weren't sure about. Don't stop to ask — log it. We'll review the log together at the end.

### Reference-anchored work
> Before writing anything: read `[file/module that represents what good looks like here]`, tell me the patterns you extracted from it, and only then start. Your output should look like it was written by the same author.

### Plan, then critique your own plan
> Write the implementation plan first. Then critique it: what's the step most likely to go wrong, what does the plan assume about the codebase that you haven't verified, and what would a reviewer flag. Fix the plan, show me both versions, and wait for my go-ahead.

### Post-task quiz (knowledge consolidation)
> We're done. Quiz me on what happened: 5 questions about the decisions made, the code paths touched, and the risks accepted. Don't show answers until I respond. If I get one wrong, that's a gap I need to close before I review someone else's work in this area.

### Verification pass
> Before you tell me you're done: list every claim you made during this task (things you said work, are safe, are faster). For each, state how it was verified — test run, build output, measurement — or mark it UNVERIFIED. Then go verify the unverified ones that are cheap to check.

---

## 7. Coroutines, Flow & structured concurrency

### Leak & scope audit
> Audit every coroutine scope in `[module/app]`: GlobalScope usage, hand-rolled `CoroutineScope(...)` that nobody cancels, scopes tied to objects that outlive their purpose, and `callbackFlow` blocks missing `awaitClose`. For each finding show the leak path (what holds what, until when) and the fix: correct lifecycle scope, or an injected application-scope for work that must outlive the caller. Rank by severity — a leaked collector on a hot flow is worse than a leaked one-shot.

### Cancellation-correctness audit
> Sweep `[module]` for code that breaks cooperative cancellation: `runCatching` or broad `catch (e: Exception)` that swallows `CancellationException` without rethrowing, CPU loops with no `ensureActive()`/`yield()`, `NonCancellable` used for anything other than critical cleanup, and `suspendCancellableCoroutine` without a cancellation handler. These bugs are invisible until a screen is closed mid-operation — for the top 3, write the exact scenario where they misbehave, then fix them and add a regression test.

### Flow hygiene audit
> Review every Flow in `[feature]` against the standard pitfalls: `stateIn` with `Eagerly`/`Lazily` where `WhileSubscribed(5_000)` is right (and vice versa — a flow that must survive config change vs one that shouldn't); cold flows doing expensive work re-triggered per collector where `shareIn` is needed; UI collection not wrapped in `repeatOnLifecycle(STARTED)` / `collectAsStateWithLifecycle`; `flowOn` placed where it has no effect; and backpressure handling (`conflate`/`collectLatest`) on fast producers. Show me before/after for each and explain which subscriber-count/timing behavior changes.

### Exception topology map
> Map where exceptions actually go in our async stack: which scopes have `SupervisorJob`, where `CoroutineExceptionHandler`s are installed, where `coroutineScope` vs `supervisorScope` is used in repositories, and which `launch`ed coroutines have NO handler (those crash the app). Draw the propagation tree for `[a representative flow, e.g. sync]`. Then find mismatches: places we think an error is contained but it actually cancels a parent, and places errors vanish silently.

### Dispatcher discipline
> Check dispatcher usage across `[module]`: hardcoded `Dispatchers.IO/Main/Default` instead of injected dispatchers (untestable), blocking calls (Room without suspend, synchronous IO, `runBlocking`) on Main or Default, and `withContext(Dispatchers.Main)` where `Main.immediate` matters. Propose the injected-dispatcher convention (qualifiers + DI module) and migrate one ViewModel + its test as the template.

### Concurrency "what does this print" drill
> Generate 5 short code snippets from patterns that actually exist in our codebase (scopes, SupervisorJob, exception handlers, shared flows) where the behavior is non-obvious — what prints, what cancels, what crashes. Quiz me one at a time; after each answer, show the real behavior and where in our codebase this exact pattern lives. Tailor difficulty to what I get wrong.

### Race forensics
> `[Bug: e.g. state occasionally wrong / crash on rotation / duplicate network call]`. Treat this as a race until proven otherwise: identify every piece of mutable state reachable from more than one coroutine in `[flow]`, the ordering assumptions between them, and where a config change or rapid re-entry breaks those assumptions. Propose the fix at the state-model level (single source of truth, `MutableStateFlow.update`, mutex, or restructure) — not a sprinkled `synchronized`.

---

## 8. DI mastery (Hilt/Dagger)

### Scope correctness audit
> Audit our Hilt setup: everything in `SingletonComponent` that doesn't need to be (memory held for app lifetime), unscoped bindings that are expensive to construct and get rebuilt per-injection, `ActivityRetainedComponent` vs `ViewModelComponent` misuse, and any binding that captures an Activity/Fragment context in a longer-lived scope (leak). Output a table: binding → current scope → correct scope → why.

### DI as a build-speed problem
> Analyze how our DI setup affects build times: Hilt aggregating tasks forcing cross-module recompilation, `@InstallIn(SingletonComponent)` modules scattered across feature modules, kapt vs KSP for Dagger. Measure a representative incremental build before proposing anything, then quantify the top fix.

### Test seam audit
> For `[feature]`, evaluate how testable our DI graph makes it: can I replace `[dependency]` with a fake without `@BindValue` gymnastics? Are dispatchers and clocks injected? Are there `object` singletons or static access bypassing DI entirely (the untestable back doors)? List the seams that are missing, and add the two most valuable ones.

---

## 9. Testing concurrency (where most suites rot)

### Flaky coroutine test clinic
> Find the flaky or slow tests in `[module]` and diagnose against the usual suspects: real `delay`s instead of `runTest` virtual time, `viewModelScope` not participating in the test's `TestScope` (needs injected dispatcher via `Dispatchers.setMain` or a scope seam), `UnconfinedTestDispatcher` used where `StandardTestDispatcher` ordering matters, missing `backgroundScope` for never-completing collectors, and Turbine collectors not consumed fully. Fix the worst one and explain the mechanism, not just the patch.

### Test the cancellation, not just the happy path
> For `[ViewModel/repository]`, write tests that most teams skip: what happens when the coroutine is cancelled mid-operation (screen closed), when the flow's subscriber count drops to zero and returns, and when an upstream error occurs during collection. Use `runTest` + Turbine. If any test is hard to write, that's a design smell — tell me what to change.

---

## 10. Force multipliers (turn one-off wins into permanent ones)

### Review comment → lint rule
> I keep leaving this review comment: `[e.g. "don't use runCatching around suspend calls without rethrowing CancellationException"]`. Turn it into an enforced rule: write a custom lint check (or Konsist/detekt rule — pick what fits our setup), with tests, wired into CI. I never want to type this comment again.

### CLAUDE.md from tribal knowledge
> Scan this repo and interview me to produce a `CLAUDE.md` that makes every future AI session better: build commands, module conventions, DI patterns, coroutine/Flow conventions, forbidden patterns, how to run tests. Ask me only for what you can't infer from the code. Keep it under 100 lines — it's context, not documentation.

### Repeated prompt → skill
> I've now asked you variations of `[task, e.g. the flow hygiene audit]` three times. Turn it into a reusable skill/slash command with the checklist baked in, so the team can run it consistently.

### Fix-until-green loop
> `[Task]`. Work in a loop: make a change, run `[test command]`, read the failures, adjust. Don't ask me anything unless you're about to change the approach fundamentally. Report at the end: what you tried, what failed, final state, and anything in the UNKNOWNS log.

### Git historian
> This code in `[file]` looks wrong/weird. Before proposing a "fix", act as a git historian: use `git log -p` and blame to reconstruct why it's shaped this way — the bug it fixed, the constraint that existed. Then tell me whether the constraint still holds. (Chesterton's fence, automated.)

### Pre-mortem
> We're about to ship `[change]`. It's 6 weeks later and it caused an incident. Write that incident's postmortem now: the most plausible failure mode given this codebase, the alert that fired (or didn't), the rollback that was or wasn't possible. Then tell me the cheapest change today that makes that postmortem fiction.
