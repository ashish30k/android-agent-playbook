---
description: Full app size audit — dex/resources/native breakdown, R8, 16KB alignment
---
Audit app size: run bundletool/APK analyzer on the current build, break down by dex/resources/native/assets, flag the top 10 contributors, check R8 config for over-broad keep rules, find assets that should be CDN-served instead of bundled, and audit native .so files including 16 KB page-size alignment compliance. Deliver a findings table plus the 3 highest-ROI reductions with savings verified by measurement, not guessed.
