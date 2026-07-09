---
description: Audit Hilt/Dagger scoping for leaks, bloat, and wrong lifetimes
argument-hint: [module or whole app]
---
Audit the DI setup in $ARGUMENTS: everything in SingletonComponent that doesn't need to be (memory held for app lifetime), unscoped bindings that are expensive to construct and rebuilt per-injection, ActivityRetainedComponent vs ViewModelComponent misuse, and any binding capturing an Activity/Fragment context in a longer-lived scope (leak). Output a table: binding → current scope → correct scope → why.
