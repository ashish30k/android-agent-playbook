---
description: Surface unknown unknowns before working in an unfamiliar module
argument-hint: [module or subsystem]
---
I'm about to work on $ARGUMENTS which I don't know well. Do a blind spot pass: scan the module and tell me my unknown unknowns — hidden invariants, non-obvious owners/consumers, gotchas in the build setup, tests that guard behavior I might break, and tribal-knowledge patterns (naming conventions, DI scoping, threading rules) I'd violate as a newcomer. Then tell me what questions I should be asking you that I haven't.
