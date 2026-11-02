# Code review — FlexLauncher fork (2026-11-02)

Scope: every file in `app/src` (30 Kotlin files, 2,540 lines), the manifest, resources,
`app/build.gradle` and the CI workflow. Method: full read, `./gradlew lintDebug`
(3 errors, 100 warnings), and behaviour on a Carlinkit Ultra AI Box (Android 15, 2374×1016).

**Verdict.** It builds and the parts that exist work, but the app is a skeleton wearing a
115-feature README. Roughly half of what the manifest declares is a placeholder, several
paths are unreachable, and the multi-user "security" is decorative — passwords are stored,
never checked. Split screen is the only feature that is genuinely complete.

## What actually works (verified on the box)

- first-run setup (single-user path), app grid with icons, launching apps, long-press menu
  (open / split / repeat last split / app info)
- split screen: freeform engine, picker, remembered pair, 63% panel default
- All Apps and Search dialogs, Manage Profiles list, dock intents (dialer, SMS, browser)
- profile switching (without any authentication — see P0-2)

## P0 — correctness and security

1. **Manifest points at a class that does not exist.**
   `AndroidManifest.xml:86` declares `.ui.appmanager.AppManagerActivity`; the file lives at
   `ui/settings/AppManagerActivity.kt`. Lint reports it as an error; any future navigation
   would crash with `ClassNotFoundException`.

2. **Profile passwords are never verified.** `SetupActivity` collects them and
   `UserRepository.hashPassword()` (line 54) stores a plain SHA-256; `UserSelectionActivity`
   switches profile on a single tap (`select()`, line 76) and `verifyPassword()` has no
   caller anywhere. The README's "password-protected profiles" does not exist.

3. **`MainActivity.setupUI()` runs inside the settings observer** (`MainActivity.kt:43-59`).
   Every settings emission re-inflates the whole layout, builds a new adapter and registers
   another `currentUser` observer. Profile switching or any future settings write multiplies
   observers and resets scroll position.

4. **The multi-user entry point can loop or show nothing.** The same observer starts
   `UserSelectionActivity` whenever `currentUserId == 0` (line 52); if the user backs out,
   the next settings change re-launches it. Worse, setup's multi-user path lets you press
   Finish with zero profiles, and `UserSelectionActivity` then renders an empty screen with
   no way forward.

5. **`MainViewModel.loadAppsForUser()` leaks observers** — `observeForever` (line 63) is
   never removed, so each profile gets another live observer for the app list. The `syncing`
   guard (line 58) is only cleared on success; one failed sync disables future auto-sync.

6. **Weak password storage.** Unsalted SHA-256 (`UserRepository.kt:54`) — the README claims
   "salt per user". Even with salted hashing, this is a 4-digit PIN on a car box; the point
   is to not pretend it is protection.

7. **Setup overwrites settings.** `SetupActivity.finishSetup()` (line 151) calls
   `completeSetup()`, which inserts a fresh `LauncherSettings` with defaults into row id 1,
   silently resetting anything already stored if setup is ever re-run.

## P1 — declared but inert

| Claim / component | Reality |
|---|---|
| `PackageChangeReceiver` "keeps the DB in sync" (`receivers/…:10`) | logs the package name and returns; installs and uninstalls never reach the database, so new apps do not appear until the profile is empty and re-synced |
| `BootReceiver` (line 12) | no-op |
| Settings / App Manager / Profiles / Themes activities | `PlaceholderActivity` stubs, and **nothing navigates to them** — no menu, no long-press-home action; unreachable code |
| Gestures (`GestureDetector`, `MainActivity`) | fling and double-tap are detected, then every handler is `TODO`; swiping does nothing |
| Clock widget | `date_text` (`activity_main.xml:68`) is never assigned — the date line is empty |
| Notification badges | `notification_badge` is `GONE` and nothing ever sets it |
| Categories (`AppCategory`, `CategoryDao`, `DefaultCategories`) | referenced only by the database definition — no category is created, assigned or rendered |
| `LauncherSettings` (35 fields) | only `isMultiUserMode` and `currentUserId` are read; themes, icon size, grid size, gestures, search provider, clock options, backup are inert |
| Wallpaper permissions (`BIND_WALLPAPER`, `SET_WALLPAPER`, `SET_WALLPAPER_HINTS`) | no wallpaper code |
| App cloning (`ShizukuManager.cloneApp`) | runs `pm install-existing --user`, which installs an already-present package for a user — it does not clone or flavour anything |
| `AppRepository` | `toggleFavorite`, `toggleHidden`, `uninstallApp`, `getAppsByCategory`, `getFavoriteApps`, `getRecentApps`, `getMostUsedApps` have no callers |

## P2 — quality and maintenance

- **Unused dependencies**: glide, lottie, work-runtime, biometric, colorpicker, viewpager2,
  preference, gson — zero imports between them. APK is 6.1 MB.
- **61 unused resources** (45 strings, 16 colours), 7 hardcoded strings in SetupActivity and
  dialogs, 5 overdraw warnings, missing content descriptions.
- `GlobalScope.launch` in `AppRepository.launchApp` (line 186); an ad-hoc
  `CoroutineScope(Dispatchers.Main)` in `SplitLauncher` (line 85) that outlives the activity.
- `ShizukuManager.executeShizukuCommand` reads both streams **before** `waitFor()` and has no
  timeout — a command that never exits blocks the coroutine forever. The permission listener
  is never removed (`cleanup()` has no caller).
- `MainActivity.onBackPressed()` (line 318) overrides without calling super (lint error) and
  uses a deprecated API.
- `app/build.gradle` references `proguard-rules.pro`, which does not exist (harmless while
  `minifyEnabled false`, a trap the day someone enables it).
- Room: `fallbackToDestructiveMigration()` on version 1 and `exportSchema = false` — no
  schema history for the first real migration.
- Junk file: `data/model/Del.me` (empty).
- Tests: 3 unit tests, all geometry. No database, view-model or repository coverage.
- CI publishes a **debug-signed** APK as a release on every push to `main`, with no lint or
  test gate.
- Manifest requests `QUERY_ALL_PACKAGES` and storage permissions that nothing uses.

## Fixed in d5cfe1f (2026-11-02)

- P0-1 … P0-7: manifest class moved to `ui.appmanager`, password prompt + PBKDF2 hashing
  (legacy SHA-256 still verifies), `setupUI()` runs once, multi-user lock overlay instead of
  the default-profile fallback, `refreshCurrentUser()` on settings change, observers removed
  on switch/clear, `completeSetup()` preserves settings, `OnBackPressedCallback`, unused
  wallpaper permissions dropped. `lintDebug` is now 0 errors.
- Home grid: columns from screen width (8 on the car box), compact clock, 52 dp tiles.
- Verified on the box: lock → picker → wrong password (toast, stays locked) → correct
  password (grid as that profile).

## Fixed in c289777 (P1 batch)

- `PackageChangeReceiver` resyncs and prunes on install/replace/uninstall;
  `BootReceiver` does the same at boot.
- Real Settings screen (top-bar overflow) with show-clock / 24-hour / app-names
  switches, applied live by the home screen; the three unreachable stub screens and
  `PlaceholderActivity` are deleted, as is the fake `pm install-existing` "cloning".
- Fling-up-at-top opens All Apps (recorded on touch-down so the scroll cannot defeat it);
  the old launcher-wide detector never received the events.
- Date line filled; long-press Hide/Uninstall; app labels can be hidden; the never-set
  notification badge is removed.
- Not verified live: install/uninstall resync — the box's package installer wedged
  during the test (installs hang, no package lands), so it needs a retry after a reboot.

## Fixed in 1ae2041 (P2 batch)

- Dependencies: glide, lottie, work-runtime, biometric, colorpicker, viewpager2,
  preference and gson removed (0 imports). Unsigned release APK 6.1 MB → 4.7 MB.
- Resources: 44 unused strings, 17 unused colours and the never-set badge drawable
  removed — `UnusedResources` is now 0, lint warnings 108 → 43, errors 0.
- Model: `AppCategory`/`CategoryDao`/`DefaultCategories` deleted (nothing referenced
  them); `LauncherSettings` trimmed from 35 fields to the 6 that exist; `AppInfo` and
  `User` lost their never-read columns. DB version 2 with destructive fallback
  (pre-release fork; local data is wiped on upgrade).
- `ShizukuManager` shell helper: 10 s timeout, process always destroyed.
- CI: lint + tests + assembleDebug, artifact only; it no longer publishes a debug-signed
  "release" on every push to main.
- README: status block stating what ships, pointing at this review.

## Suggested order

1. **P0 batch (small, high value):** fix the manifest class, add a password prompt on profile
   selection (or delete the fields and the claim), make `setupUI()` run once, stop the
   `UserSelectionActivity` re-launch loop, replace `observeForever` with a lifecycle-aware
   observer, stop `completeSetup()` clobbering settings.
2. **P1 batch (cheap):** make `PackageChangeReceiver` resync, fill the date, either wire the
   four stub screens into a menu or delete them, delete the dead repository methods, and
   either implement or remove the gesture detector.
3. **P2 batch:** prune dependencies and resources to cut the APK, add the Shizuku timeout,
   take lint to zero, gate CI on `testDebugUnitTest` + `lintDebug`.

Anything not in this list (themes, folders, widgets, icon packs, widgets, backup, biometrics)
is simply not implemented; the README should say so rather than list it as a feature.
