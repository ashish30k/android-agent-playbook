# Security Review Checklist (MASVS v2.1 / MAS-L2 aligned)

Banking apps are reviewed to MAS-L2: defense-in-depth, assume the device is hostile. Each rule: what to check → why it matters.

## STORAGE (MASVS-STORAGE)
- Sensitive data (tokens, PAN, account numbers, PII, keys) never in plain SharedPreferences, files, or external storage. Use Keystore-backed encryption (EncryptedSharedPreferences or equivalent). *Why: /data/data is readable on rooted devices and via backup extraction.*
- Backup rules: `android:allowBackup`, `dataExtractionRules`/`fullBackupContent` must exclude sensitive stores. *Why: adb backup / cloud restore exfiltrates anything not excluded.*
- Keystore keys for high-value operations: `setUserAuthenticationRequired(true)`, StrongBox where available. Check nothing logs or serializes key material.
- Check caches, WAL/journal files, temp files, and downloaded documents for sensitive content lifetime.

## CRYPTO (MASVS-CRYPTO)
- No homegrown crypto, no ECB mode, no static/hardcoded IVs, keys, or seeds anywhere (including tests copied to main, BuildConfig, string resources). AES-GCM with Keystore-generated keys is the default answer.
- `SecureRandom` for anything security-relevant; never `Random`. No MD5/SHA-1 in security contexts.

## NETWORK (MASVS-NETWORK)
- No `cleartextTrafficPermitted="true"` anywhere, including debug overrides that could leak to release variants.
- Certificate pinning present for bank endpoints, with backup pins and a rotation story. *Why: pinning without backup pins is a self-inflicted outage.*
- No custom `TrustManager`/`HostnameVerifier` that weakens validation (the classic "trust-all to fix a dev-env error" that ships).
- No secrets/tokens/PII in URLs or query params. *Why: they end up in server logs, proxies, and referrer headers.*

## LOGGING & PII (MASVS-PRIVACY)
- No `Log.*`/`println` with account data, tokens, amounts+identifiers, phone/email. Check `toString()` of models included in logs and exception messages carrying PII.
- Release builds strip debug logging (Timber tree / R8 assumption rules) — verify the mechanism exists, don't assume.
- Crash reporting and analytics: no PII in custom keys, breadcrumbs, or event properties. Check what new third-party SDKs receive. *Why: analytics is the most common unintentional PII pipeline to third parties.*

## PLATFORM & IPC (MASVS-PLATFORM)
- Manifest diff: any newly `exported="true"` component is a finding unless justified and permission-guarded.
- Deeplinks/App Links: all params treated as hostile input — validated, never directly driving navigation to authenticated screens or prefilling transactions.
- `PendingIntent`: `FLAG_IMMUTABLE` unless mutability is justified.
- WebView: no `addJavascriptInterface` exposing sensitive APIs, no `setAllowFileAccess`, origin allow-list enforced, no loading user-influenced URLs into privileged WebViews.
- Clipboard: no sensitive values placed on it; OTP/account fields cleared/expired if copy is a feature.

## UI LEAKAGE
- `FLAG_SECURE` on screens showing balances, cards, statements (verify the Compose/Window path actually applies it). *Why: screenshots, screen recording, and the recents thumbnail all leak.*
- Sensitive input fields: no keyboard learning/autofill leakage (`importantForAutofill`, input types), `filterTouchesWhenObscured` where tapjacking matters.

## AUTH & SESSION (MASVS-AUTH)
- `BiometricPrompt` must gate a `CryptoObject` operation, not just flip a boolean on callback. *Why: callback-only biometrics is bypassable with instrumentation.*
- Logout invalidates server session and wipes local tokens/caches. Step-up re-auth for high-risk operations (payee add, limit change, transfer above threshold).
- Session/auth state not held in mutable statics reachable before auth completes.

## RESILIENCE (MASVS-RESILIENCE)
- Play Integrity / root & tamper signals verified server-side; client-only checks are UX hints, not gates. *Why: client checks are patchable; the server decision is what counts.*
- R8 enabled; keep-rule diffs reviewed (over-broad keeps unshield internals). `debuggable=false`, no leftover debug endpoints, test flags, or bypass toggles behind remote config.
- New dependencies: justify, check provenance, prefer dependency verification metadata. *Why: supply chain is the current front line.*
