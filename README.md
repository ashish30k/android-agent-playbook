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
  | `/code-review` | Banking-grade PR review (security + money + prod-readiness) |

- **skills/android-bank-code-review/** — full review workflow for banking apps: diff triage into lanes, OWASP MASVS v2.1 (MAS-L2) security checklist, money-correctness rules (BigDecimal, rounding, idempotency), and prod-readiness checks (process death, ANRs, migrations, rollout safety). Every rule carries its "why" so it teaches mixed-level reviewers.
- **skills/android-concurrency-auditor/** — a deep 5-pass audit skill for coroutine/Flow code: scope leaks, broken cancellation (the `runCatching`/`CancellationException` trap), flow hygiene (`stateIn`/`WhileSubscribed`), dispatcher discipline, exception topology. Copy to `~/.claude/skills/` — it triggers automatically when you ask about leaks, cancellation, or flow bugs.

## Install

```bash
git clone https://github.com/ashish30k/android-agent-playbook.git
cp -r android-agent-playbook/commands/* your-project/.claude/commands/
cp -r android-agent-playbook/skills/* ~/.claude/skills/
```
