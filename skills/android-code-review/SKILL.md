---
name: android-code-review
description: Full code review for any Android project — security, architecture, concurrency/ViewModels, UI/Compose, accessibility, i18n, permissions, background work, build, and release safety. Use this skill whenever the user asks to review a PR, diff, branch, commit, or file in an Android/Kotlin project — including "review this change", "check my PR", "is this safe to ship", "look at this code" — regardless of app domain. Applies stricter money/PII rules automatically when the project handles payments or sensitive data.
---

# Android Code Review

Review changes the way a staff engineer would: findings that can cause a prod incident are blocking; everything else is negotiable. Reviews must teach — state the rule, the *why*, then the fix. A comment that just says "don't do this" creates the same bug next quarter.

## Step 0 — Profile the project (once per session)

Skim the manifest, root Gradle files, and package names to establish:
- **Domain sensitivity**: does the app handle money, health, or regulated PII? If yes, apply the strict profile — §Money in general-review.md becomes mandatory, and security findings default one severity higher.
- **Stack**: Compose vs Views (or both), single vs multi-module, DI framework — so you review against the conventions the project actually uses, not generic ones. If the repo has a CLAUDE.md or CONTRIBUTING/style docs, they override these checklists where they conflict.

## Step 1 — Gather and triage the diff

Get the change set (`git diff <base>...HEAD`, or the PR/files the user points at). Classify every changed file into one or more lanes:

| Lane | Triggers | Reference |
|---|---|---|
| **Security** | auth/session, networking, storage, crypto, WebView, deeplinks/IPC, `AndroidManifest.xml`, ProGuard/R8 rules, dependency changes | `references/security-review.md` |
| **Concurrency** | coroutines, Flow, ViewModels, threading, new async work | `references/general-review.md` §Concurrency |
| **Architecture** | new modules, public API changes, DI modules, cross-module imports | `references/general-review.md` §Boundaries, §DI |
| **Prod-readiness** | state/lifecycle, error handling, offline/retry, DB schema or data-format changes, feature flags, startup path | `references/general-review.md` §Lifecycle, §Errors, §Data, §Release |
| **Money** (strict profile) | amounts, balances, rates, fees, transactions, formatting | `references/general-review.md` §Money |
| **UI & UX** | composables, layouts, custom views, themes, strings/resources, navigation | `references/ui-platform-review.md` §UI, §Accessibility, §i18n |
| **Platform** | permissions, notifications, services/WorkManager, broadcast receivers, alarms, target/min SDK changes | `references/ui-platform-review.md` §Platform, §Compatibility |
| **Routine** | none of the above | review inline |

Manifest, ProGuard, and dependency changes are ALWAYS security lane — that's where quiet catastrophes live.

## Step 2 — Expand beyond the diff (this is what makes it a real review)

The diff shows what changed; most bugs live in how unchanged code reacts to it. Before judging anything:

- Read the **full files** around each hunk, not just the changed lines — the bug is usually the interaction between new code and the untouched line twenty lines up.
- For every changed public/internal symbol, **find the call sites** (grep for the name and string references, not just imports) and check whether the change breaks their assumptions: nullability, error contract, threading/main-safety, blocking behavior, ordering, units, idempotency.
- Changed a function's suspend-ness, dispatcher, or failure behavior → re-check **every caller** against the new contract, including test fakes.
- Changed an interface/abstract class → check **all implementations**; changed a shared Flow or state → check every collector and any subscriber-count-dependent behavior.
- Renamed/deleted anything → search for reflection, DI wiring, deeplink routes, ProGuard rules, and remote-config/flag keys that reference it by string.
- New usage of an **existing utility** → skim that utility once; reviews regularly approve correct-looking calls into subtly broken helpers.

Budget: for a typical PR this is 5–15 extra file reads. Spend them — a review that only reads the diff is a formatting check.

## Step 3 — Review against the checklists

Work through the triggered sections. For each finding, verify before asserting: confirm the issue is real in this context, not just pattern-matched. If you can't confirm (e.g., can't see the server side of a check), mark it as a question, not a finding.

## Step 4 — Output format

```
## Review: <branch/PR>

**Verdict**: APPROVE / APPROVE WITH COMMENTS / REQUEST CHANGES

### Findings
[BLOCKER] file:line — <rule violated>
  Why it matters here: <one or two sentences, concrete to this code>
  Fix: <specific change, code snippet if short>

[MAJOR] ...   [MINOR] ...   [QUESTION] ...

### What I verified vs. assumed
<claims checked against actual code vs. things needing author confirmation>

### Teaching note (optional, max one)
<if one finding represents a pattern worth a short explanation for the team, expand it here>
```

Severity rules: **BLOCKER** = security violation, money/data-correctness bug, crash/data-loss risk — never waved through. **MAJOR** = correctness bug, leak, ANR source, or broken cancellation that will bite in production. **MINOR** = maintainability, convention drift, missing a11y/i18n on new UI. **QUESTION** = needs author context; never guess on security questions.

Do not pad the review. Five verified findings beat twenty speculative ones. Skip style commentary entirely — lint owns style.

## Step 5 — Close the loop

If any BLOCKER represents a repeatable pattern, end by proposing the lint/detekt/Konsist rule that would catch it mechanically (one line, offer to write it). Guidelines that live only in review comments decay; rules don't.
