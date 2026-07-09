---
description: Audit Gradle/AGP setup against current best practice with measured impact
---
Audit this project's build setup against current best practice (AGP 9.x, Gradle 9, configuration cache, built-in Kotlin support). Check: convention plugins vs copy-pasted build logic, api vs implementation dependency hygiene, configuration-cache and build-cache compatibility, unnecessary kapt (should be KSP), and module graph shape (depth, bottleneck modules). Produce a prioritized fix list with estimated build-time impact — and verify at least the top claim by running a build scan or gradle --profile.
