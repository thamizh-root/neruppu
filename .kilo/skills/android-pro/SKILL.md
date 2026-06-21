---
name: android-pro
description: Android production engineering guardrails for Kotlin, Compose, and Android projects.
---
# Kotlin Android Pro Max — Core Integrity

## Phase 0: Mandatory Discovery & Clarification Gate (BLOCKING)

**Rule: No implementation code is written until every applicable question below has been asked and answered.** A feature request is not "ready" just because it has a name and a one-line description. If the answers aren't known, they must be asked — guessing on any of these is itself a Priority 0 violation, because a wrong guess in lifecycle, state, or security design is expensive to unwind later.

This phase produces no code, no stubs, no scaffolding — only questions and confirmed answers.

**How to ask:** Don't interrogate — discuss. Each question should come with a stated opinion or default recommendation, the way two engineers would whiteboard a feature before either writes a line of code. Example: *"Should this survive process death? My take is yes — losing a half-filled form on a config change is a bad experience, so I'd lean toward `SavedStateHandle` by default. Does that match what you had in mind, or is this view truly disposable?"* This keeps the back-and-forth feeling collaborative rather than like a form to fill out, and gives the other person something concrete to agree with, correct, or override.

**Ask only what's relevant.** The list below is a menu, not a script — work through it mentally and only surface the questions that actually touch this feature. A pure UI change (restyling a component, adjusting a layout, tweaking an animation) has nothing to do with Room migrations, pagination strategy, or conflict resolution — asking those is noise, not diligence, and trains the other person to tune out the discovery step entirely. Roughly: UI-only work mostly draws from Compose surface, accessibility, i18n/RTL, theming, large-screen/foldable, and edge-to-edge — maybe a third of the list, not all of it. Data-layer or background-work features pull in a different, larger subset (caching, concurrency, migrations, conflict resolution). Match the questions to the layer actually being touched.

### Questions to ask before touching the keyboard

1. **Scope & UX** — What is the exact user-facing behavior, including empty, loading, and error states? Is this a new screen, a modification to an existing one, or a background-only feature with no UI?
2. **Lifecycle ownership** — Should this state survive configuration change only, or also process death (i.e., does it need `SavedStateHandle`)? Is it scoped to a single screen, a nav-graph, or the whole app (`Activity` vs `ViewModel` vs `Application` scope)?
3. **Data origin** — Does this feature read or write to network, disk, or both? Is there an existing repository/data source to extend, or does a new one need to be created?
4. **Caching & offline behavior** — Should data be cached locally? What's the staleness tolerance? Does the feature need to function (even partially) with no network?
5. **Concurrency shape** — Is this a one-shot operation, a continuous stream (`Flow`), or a periodic background task? If periodic, can it be event-driven (FCM, observers) instead of polling/timers?
6. **Background execution** — If work must survive app closure, does it qualify for `WorkManager` (deferrable, guaranteed) vs `Foreground Service` (immediate, user-visible)? What are the retry/backoff requirements?
7. **Compose surface** — Does this involve a list (`LazyColumn`/`LazyRow`)? What's the stable unique key for each item? Are there values that need `remember`/`derivedStateOf` to avoid recomposition storms?
8. **Sensitive data** — Does this feature touch tokens, credentials, or PII? Does it require `EncryptedSharedPreferences`/secure `DataStore`, and should any of it be excluded from logs or backups?
9. **Permissions & system resources** — Does this need runtime permissions (location, camera, sensors)? What's the exact teardown trigger (`onStop` vs `onDestroy`) to stop the listener/stream?
10. **IPC payload** — Does this pass data via `Intent` extras or a `Bundle` (e.g., to another Activity, a Service, or another app)? Roughly how large is that payload, and should it instead go through a shared ViewModel, database, or file reference to dodge `TransactionTooLargeException`?
11. **Architecture seams** — Which existing Domain use case(s) or Repository interface(s) does this extend? Is a new interface needed, or does this risk blurring Presentation → Domain → Data boundaries?
12. **DI wiring** — Is the dependency already provided in an existing Hilt/Koin module, or does a new binding/module need to be declared?
13. **Failure modes** — What should happen on network failure, partial success, stale cache, or conflicting concurrent writes? Is silent failure acceptable anywhere, or must every failure surface to the user?
14. **Testing surface** — What are the unit-testable boundaries here (ViewModel logic, repository mapping, use case rules)? What needs to be mocked (MockK), and what state/emissions need verifying with Turbine?
15. **Rollout impact** — Does this change an existing public API/class signature that other features depend on? Does it require a feature flag, migration, or backward-compatible fallback?
16. **Accessibility** — Does this need TalkBack support, content descriptions, minimum 48dp touch targets, or correct behavior under large system font scaling? My default: yes by default, unless it's a purely decorative element.
17. **Internationalization** — Will any copy here need translation, and does the layout need to hold up under RTL languages or longer translated strings? I'd assume yes unless this is an internal/debug-only screen.
18. **Theming** — Should this respect dynamic color (Material You) and explicit dark mode, or does it intentionally break from the app's theme system? My lean: inherit the existing theme unless there's a stated reason not to.
19. **Analytics & telemetry** — Should this feature emit analytics events, and if so, is there an existing naming convention to follow? I'd rather reuse the existing event taxonomy than invent a new one.
20. **Crash & error reporting** — Should failures here surface to Crashlytics/Sentry (or whatever's wired up), or is local in-app handling enough? My instinct: anything that can silently fail in production should still get logged remotely.
21. **Navigation integration** — Is this screen reached via the existing Compose Navigation/Fragment graph, and does it need a deep link entry point? I'd default to wiring it into the existing graph rather than a standalone activity.
22. **Multi-window & foldables** — Does this need to behave correctly in split-screen or foldable hinge states, or is single, full-screen use the only supported mode for now? Worth confirming explicitly rather than assuming.
23. **Large screens & tablets** — Does the layout need adaptive panes for tablets/Chromebooks, or is a single-column phone layout acceptable for v1? My lean: ship single-column first unless tablet support is already a stated requirement.
24. **Edge-to-edge & insets** — Does this screen need explicit handling of status bar, nav bar, or display cutout insets? I'd assume yes if it's a top-level screen, no if it's a dialog/bottom sheet over an existing screen.
25. **Cold-start impact** — Could anything here add meaningful weight to app startup (eager initialization, heavy singleton construction)? My default: lazy-init anything not needed on the very first frame.
26. **Auth gating** — Does reaching this feature require the user to be logged in, or re-authenticate via biometrics for sensitive actions? Worth confirming even if it seems obvious.
27. **Privacy & consent** — Does this fall under GDPR/CCPA-style consent requirements, and is that consent already captured elsewhere in the app, or does this feature need its own prompt? I'd rather reuse existing consent state than add a second prompt.
28. **Pagination** — If this loads a list, is the dataset finite, or does it need real paging (e.g., Paging 3)? My lean: assume paging is needed for anything that could plausibly grow past ~50 items.
29. **Image & media handling** — Does this load images or media, and should it go through the app's existing Coil/Glide setup and caching policy rather than a one-off loader? Reusing the existing pipeline is my default unless there's a specific reason not to.
30. **Database migration** — If this touches Room, does the schema change require a migration, and does that migration need its own test? I'd treat any schema change as requiring a migration test by default — too risky to skip.
31. **Conflict resolution** — If this data can be written from multiple sources (offline edit plus server sync, multiple devices), what's the resolution strategy — last-write-wins, merge, or prompt the user? This one genuinely needs a decision, not a default.
32. **Build variant behavior** — Does this feature behave differently across debug/staging/release (e.g., mock vs real endpoints, feature flags)? Worth confirming rather than assuming parity.
33. **Dependency footprint** — Does this require pulling in a new third-party library, and has the APK size / long-term maintenance trade-off been weighed against rolling something smaller in-house? My lean: prefer an existing dependency already in the project over a new one for a single feature.
34. **ProGuard/R8 keep rules** — Does this introduce reflection-based models (e.g., Gson/Moshi data classes) that need explicit keep rules to survive minification? I'd flag this proactively rather than wait for a release-build crash.
35. **Min SDK / API level** — Does this rely on any API only available above the app's current `minSdk`, requiring a version check (`Build.VERSION.SDK_INT`) or a fallback path? Worth checking against the current `minSdk` before assuming an API is safe to call.

If the user's original request already answers some of these explicitly, skip re-asking those — but state the assumption back to them for confirmation before proceeding. Anything left ambiguous gets asked, not assumed.

---

## 0. Priority 0 Gate (BLOCKING)
A feature is **not done** if it violates any rule below. Treat this as a hard compilation gate.

### 0a. Zero Memory Leaks
* **Lifecycle Binding:** Tie all coroutines to `viewModelScope` or `lifecycleScope` + `repeatOnLifecycle`.
* **Static Ban:** Never store an `Activity`, `Fragment`, `Context`, or `View` in singletons, companion objects, or long-lived closures.
* **Symmetric Cleanup:** Explicitly clear every registered listener or observer in `onCleared()` or `onDestroy()`.

### 0b. High Decoupling & Absolute Readability
* **Layer Isolation:** Strictly maintain **Presentation → Domain → Data**. UI must never touch network or disk directly.
* **Beginner-Clear:** Avoid over-engineering. Write simple, declarative, and highly documented code using Unidirectional Data Flow (UDF).
* **DI Only:** Inject all dependencies via Hilt or Koin constructors. Never instantiate dependencies inline.

### 0c. Peak Performance
* **Main Thread Purity:** Hard ban on disk I/O, network requests, or heavy serialization on the main thread. Force background execution via `Dispatchers.IO`.
* **Compose Hygiene:** Use `LazyColumn`/`LazyRow` with unique `key` bounds. Use `remember` and `derivedStateOf` to kill redundant recompositions.

### 0d. Zero Battery & Resource Waste
* **Event-Driven Over Polling:** Replace all loops and timers with `FCM` push events or system-optimized `WorkManager` tasks.
* **Lifecycle Throttling:** Kill all animations, sensor listeners, location tracking, and UI collection streams instantly when the screen goes off-screen (`onStop`).

### 0e. Android Internals Mastery
* **Process Death Survival:** Use `SavedStateHandle` in ViewModels to save transient UI state so data survives OS-driven background thread termination.
* **IPC Discipline:** Keep Intent Bundles under 100KB to prevent runtime `TransactionTooLargeException` failures.

### 0f. Bulletproof Security
* **Data Masking:** Encrypt all access tokens and PII using `EncryptedSharedPreferences` or secure `Proto DataStore`.
* **Network Integrity:** Enforce strict HTTPS via Network Security Config; explicitly block cleartext traffic and strip production logs.

### 0g. Test-Gated Delivery
* **Absolute Requirement:** Every single code change — regardless of scale or simplicity — must ship with matching tests.
* **The Rule:** If you cannot write or verify the matching unit tests (JUnit, MockK, Turbine), do not write the implementation code.

---

## Pre-Delivery Checklist
```
┌──────────────────────────────────────────────────────────┐
│                  PRE-DELIVERY CHECKLIST                  │
├──────────────────────────────────────────────────────────┤
│ [ ] Discovery: All open questions asked & answered first │
│ [ ] Memory Leaks: Every scope, listener, & worker closed │
│ [ ] Decoupling: Code is clean, modular, & beginner-clear │
│ [ ] Performance: Zero Main-Thread blocks; Compose stable │
│ [ ] Battery: Active tracking & streams paused onStop()   │
│ [ ] Internals: State survives process death & rotation   │
│ [ ] Security: Tokens/PII encrypted; safe cleartext policy│
│ [ ] Tests: Written & passing, even for small variations  │
└──────────────────────────────────────────────────────────┘
```
