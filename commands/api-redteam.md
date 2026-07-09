---
description: Attack a public API design as three adversarial consumers
argument-hint: [interface/module path or paste API]
---
Review this public API I'm designing for other teams: $ARGUMENTS. Role-play three consumers: (a) a junior engineer who will misuse it in the laziest way possible, (b) a team with a legacy codebase that can't adopt coroutines/Compose, (c) a team calling it from a background process. For each: show the misuse, the failure mode, and how the API shape could prevent it. Then propose the revised API.
