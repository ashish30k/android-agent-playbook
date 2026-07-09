# UI, Accessibility, i18n, Platform & Compatibility Review

Each rule: check → why.

## UI — Compose
- State hoisted; composables stateless where feasible; `MutableState`/business logic not created inside composables that should be dumb.
- Side effects correct: `LaunchedEffect` keys actually represent restart conditions; `DisposableEffect` cleans up what it registers; no side effects in composition itself. *Why: wrong keys = effects silently not restarting (or restarting every recomposition).*
- Recomposition hygiene on hot screens: unstable lambdas/params, missing `key()` in lists, wide snapshot reads. Collect flows with `collectAsStateWithLifecycle`.
- `remember` for expensive objects; `rememberSaveable` for UI state that must survive config change and process death.

## UI — Views (where used)
- RecyclerView uses ListAdapter/DiffUtil, no `notifyDataSetChanged` on real lists. Fragment view bindings nulled in `onDestroyView`. *Why: retained binding = leaked view hierarchy per navigation.*
- Custom views implement `onSaveInstanceState` for state users would notice losing.

## UX correctness (every new screen/state)
- Loading, empty, error, and offline states all defined — no spinner-forever, no blank screen on failure.
- Back handling deliberate (predictive back compatible); keyboard/IME doesn't cover inputs; edge-to-edge insets handled (enforced since targetSdk 35).
- Dark theme and landscape/foldable don't break the screen; test at largest font scale.

## Accessibility (new UI = MINOR findings minimum, not "later")
- Interactive elements: content descriptions (or explicit null for decorative), touch targets ≥48dp, focus order/semantics merged sensibly for TalkBack.
- Contrast meets WCAG AA; state conveyed by more than color alone; no text baked into images.

## i18n & resources
- No hardcoded user-visible strings; plurals via `plurals`, not `if (count == 1)`. Positional args in format strings.
- RTL-safe: `start`/`end` never `left`/`right`; test with pseudolocale/RTL. Locale-aware date/number formatting — never string-built.
- Colors/dimens/typography from the design system, not new literals. Vectors over rasters.

## Platform
- **Permissions**: minimal set; runtime flow handles rationale AND permanent denial gracefully; feature degrades rather than dead-ends. New manifest permission = justify in PR.
- **Notifications**: channels with right importance; `POST_NOTIFICATIONS` runtime permission handled; deep links from notifications validated and process-death safe.
- **Background work**: WorkManager for deferrable-guaranteed work; foreground services declare a type (mandatory) and are the last resort; exact alarms need the special permission and a real justification; assume Doze/App Standby will delay everything else.
- **Broadcast receivers**: context-registered over manifest where possible; exported only with permission guards; no long work in `onReceive`.
- **Navigation**: back stack correct on deep-link entry; nav state survives process death; args are Parcelable/primitives, not giant objects.

## Compatibility & release
- New platform APIs gated by SDK check or androidx backport; library desugaring for java.time on old minSdk.
- targetSdk bump = review the behavior-change list for that level, item by item — this is a lane of its own, not a version number edit.
- Native libs: 16 KB page-size alignment. Play policy items (data safety form impact) when data collection changes.
