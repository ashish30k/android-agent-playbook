---
description: Design an incremental, revertible migration with a kill-switch
argument-hint: [old thing] -> [new thing]
---
Design a migration for: $ARGUMENTS. Requirements: incremental (per-module or per-screen), each step independently shippable and revertible behind a flag, old and new paths coexist, and a measurable definition of "done" so the old path actually gets deleted. Output: phase plan, a lint/Danger rule preventing new usage of the old path, and the tracking mechanism.
