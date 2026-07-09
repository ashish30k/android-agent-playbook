# android-agent-playbook

Prompts, slash commands, and skills for senior/staff Android platform work with Claude Code.

## Contents

- **[android-platform-prompts.md](android-platform-prompts.md)** — the full playbook: 44 prompts across architecture & API design, build systems, performance & app size, coroutines/Flow concurrency, DI, testing, and staff-level leverage.
- **commands/** — the curated set as slash commands. Copy into a project's `.claude/commands/` (per-repo) or `~/.claude/commands/` (everywhere):

  | Command | What it does |
  |---|---|
  | `/blind-spot-pass` | Unknown unknowns before touching an unfamiliar module |
  | `/blast-radius` | Every consumer + risk before changing an API |
  | `/arch-interview` | Claude interviews you about your spec, architecture-first |
  | `/api-redteam` | Attack an API design as 3 adversarial consumers |
  | `/migration-plan` | Incremental, revertible migration with kill-switch |
  | `/build-health` | Gradle/AGP audit with measured impact |
  | `/size-audit` | APK/AAB breakdown, R8, 16KB alignment |
  | `/di-scope-audit` | Hilt/Dagger scope correctness |
  | `/design-redteam` | 3 hostile reviewers for your design doc |
  | `/pre-mortem` | The incident postmortem, written before shipping |
  | `/git-historian` | Why weird code is shaped that way (Chesterton's fence) |
  | `/delegation-packet` | Handoff packet for another engineer |
  | `/review-to-lint` | Repeated review comment → enforced lint rule |
  | `/code-review` | Full PR review — all lanes, strict mode for money/PII apps |

- **skills/android-code-review/** — full review workflow for any Android project: profiles the project (strict money/PII mode auto-applies for fintech), triages the diff into lanes, traces impact beyond the diff (call sites, implementations, contract breaks in unchanged code), and reviews against checklists covering OWASP MASVS v2.1 security, money-correctness, concurrency/ViewModels, UI/Compose, accessibility, i18n, permissions, background work, and release safety. Every rule carries its "why" so it teaches mixed-level reviewers.
- **skills/android-concurrency-auditor/** — a deep 5-pass audit skill for coroutine/Flow code: scope leaks, broken cancellation (the `runCatching`/`CancellationException` trap), flow hygiene (`stateIn`/`WhileSubscribed`), dispatcher discipline, exception topology. Copy to `~/.claude/skills/` — it triggers automatically when you ask about leaks, cancellation, or flow bugs.

## Install

**As a plugin (recommended)** — in Claude Code:

```
/plugin marketplace add ashish30k/android-agent-playbook
/plugin install android-agent-playbook@android-agent-playbook
```

All commands and skills become available in every project.

**Manual copy** (per-project):

```bash
git clone https://github.com/ashish30k/android-agent-playbook.git
cp -r android-agent-playbook/commands/* your-project/.claude/commands/
cp -r android-agent-playbook/skills/* ~/.claude/skills/
```

**CI**: copy `.github/workflows/claude-review.yml` into your repo and set `ANTHROPIC_API_KEY` in secrets for automated PR review.

## Validation

Both skills are benchmarked against fixtures with planted bugs (with-skill vs. no-skill baseline, graded on objective assertions):

| Skill | With skill | Baseline | Notes |
|---|---|---|---|
| android-concurrency-auditor | 100% (14/14) | 85.7% | Baseline missed the runCatching/CancellationException trap and dispatcher testability in symptom-driven debugging |
| android-code-review | 100% (11/11) | 81.8% | Both caught all planted bugs incl. a cross-file contract break; baseline lacked verified-vs-assumed discipline and the lint-rule close-loop |

Review-skill fixture (a checkout PR with 9 planted issues across all lanes) ships in `skills/android-code-review/evals/` — rerun it after any guideline change.
