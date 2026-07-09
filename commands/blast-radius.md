---
description: Map every consumer and risk before changing an API/class/module
argument-hint: [API, class, or module to change]
---
I'm planning to change $ARGUMENTS. Before I touch anything: find every consumer (including reflection, DI graph wiring, service loaders, and generated code), classify them by risk, and identify owning teams via CODEOWNERS/git blame. Which consumers would break silently rather than at compile time? Verify your consumer list is complete by searching for string references and fully-qualified names, not just imports.
