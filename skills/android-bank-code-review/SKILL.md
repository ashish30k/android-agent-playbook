---
name: android-bank-code-review
description: Structured code review for banking/fintech Android apps in multi-module codebases, aligned to OWASP MASVS v2.1 (MAS-L2). Use this skill whenever the user asks to review a PR, diff, branch, or file in an Android project that handles money, credentials, or personal data — including requests phrased as "review this change", "check my PR", "is this safe to ship", or any review touching auth, networking, storage, WebView, deeplinks, manifest, ProGuard/R8, or Gradle files.
---

# Banking Android Code Review

Review changes the way a staff engineer at a bank would: security and money-correctness are blocking; everything else is negotiable. The team is mixed-level, so every comment must teach — state the rule, then the *why*, then the fix. A comment that just says "don't do this" creates the same bug next quarter.

## Step 1 — Gather and triage the diff

Get the change set (`git diff <base>...HEAD`, or the PR/files the user points at). Classify every changed file into one or more lanes:

| Lane | Triggers | Reference to read |
|---|---|---|
| **Security-critical** | auth/session/biometric code, networking, storage, crypto, WebView, deeplinks/IPC, `AndroidManifest.xml`, `network_security_config`, ProGuard/R8 rules, Gradle dependency changes | `references/security-review.md` |
| **Money-handling** | amounts, balances, rates, fees, transactions, formatting | `references/general-review.md` §Money |
| **Concurrency** | coroutines, Flow, ViewModels, threading, new async work | `references/general-review.md` §Concurrency |
| **Architecture** | new modules, public API changes, DI modules, cross-module imports | `references/general-review.md` §Boundaries, §DI |
| **Prod-readiness** | state/lifecycle code, error handling, offline/retry logic, DB schema or data-format changes, feature flags, anything touching startup | `references/general-review.md` §Lifecycle, §Errors, §Release |
| **Routine** | none of the above | review inline, no reference needed |

Read only the reference sections the diff actually triggers. Manifest, ProGuard, and dependency changes are ALWAYS security lane — these are where the quiet catastrophes live (an accidentally-exported activity, a keep rule that unshields internals, a new SDK that phones home).

## Step 2 — Expand beyond the diff (this is what makes it a real review)

The diff shows what changed; most bugs live in how unchanged code reacts to it. Before judging anything:

- Read the **full files** around each hunk, not just the changed lines — the bug is usually the interaction between new code and the untouched line twenty lines up.
- For every changed public/internal symbol, **find the call sites** (grep for the name and string references, not just imports) and check whether the change breaks their assumptions: nullability, error contract, threading/main-safety, blocking behavior, ordering, units/currency, idempotency.
- Changed a function's suspend-ness, dispatcher, or failure behavior → re-check **every caller** against the new contract, including test fakes.
- Changed an interface/abstract class → check **all implementations**; changed a shared Flow or state → check every collector and any subscriber-count-dependent behavior (`WhileSubscribed`, `shareIn` replay).
- Renamed/deleted anything → search for reflection, DI wiring, deeplink routes, ProGuard rules, and remote-config/flag keys that reference it by string.
- New usage of an **existing utility** → skim that utility once; reviews regularly approve correct-looking calls into subtly broken helpers.

Budget: for a typical PR this is 5–15 extra file reads. Spend them — a review that only reads the diff is a formatting check.

## Step 3 — Review against the checklists

Work through the triggered sections. For each finding, verify before asserting: confirm the issue is real in this context, not just pattern-matched. If you can't confirm (e.g., can't see the server side of an integrity check), mark it as a question, not a finding.

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

Severity rules: **BLOCKER** = security/compliance violation, money-correctness bug, or data-loss risk — never waved through. **MAJOR** = correctness bug, leak, or broken cancellation that will bite in production. **MINOR** = maintainability, convention drift. **QUESTION** = needs author context; never guess on security questions.

Do not pad the review. Five verified findings beat twenty speculative ones. Skip style commentary entirely — lint owns style.

## Step 5 — Close the loop

If any BLOCKER represents a repeatable pattern, end by proposing the lint/detekt/Konsist rule that would catch it mechanically (one line, offer to write it). Guidelines that live only in review comments decay; rules don't.
