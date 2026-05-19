# AGENTS.md

This guide applies to the whole repository. It is written for coding agents that
need to make focused, buildable changes without rediscovering the project shape
each time.

## Project Snapshot

LibChecker is a Kotlin Android app for inspecting installed apps and external
APK/APKS/XAPK/HAP files. It surfaces native libraries, DEX/static libraries,
components, permissions, signatures, SDK metadata, snapshots, rule matches, and
statistics. The README lists support for Android 7.0 through Android 16.

The Gradle build has two included modules:

- `:app` is the Android application.
- `:hidden-api` is a compile-only Android library containing hidden platform API
  stubs and Rikka Refine annotations. Do not put runtime app logic here.

The included `build-logic` build defines shared Android conventions and custom
plugins used by the main build.

## Repository Map

- `app/src/main/kotlin/com/absinthe/libchecker/` is the app source root.
- `app/src/main/res/` contains XML layouts, menus, themes, drawables, strings,
  preferences, backup rules, FileProvider paths, and split-window rules.
- `app/src/main/proto/LibChecker.proto` drives protobuf-generated code.
- `app/src/main/aidl/` defines IPC interfaces for app services.
- `app/src/foss/` and `app/src/market/` are flavor source sets. Keep matching
  APIs in both flavors when touching telemetry or HarmonyOS delegates.
- `app/schemas/` stores exported Room schemas. Update it when Room schema changes.
- `app/proguard-rules.pro` and `app/aapt2-resources.cfg` affect release builds.
- `app/foss/`, `app/market/`, `app/build/`, `build/`, `.gradle/`, and `.kotlin/`
  are build outputs or local state unless a task explicitly targets artifacts.
- `gradle/libs.versions.toml` is the dependency and plugin version catalog.
- `.github/workflows/android.yml` runs formatting and release build checks.
- `.github/copilot-instructions.md` has additional high-level AI guidance, but
  this file is the practical root guide for agent work.

## Build And Validation

Use the Gradle wrapper. On this Windows workspace, prefer `.\gradlew.bat`; CI
uses `./gradlew`.

Common commands:

- `.\gradlew.bat spotlessCheck` checks formatting and is the fast CI gate.
- `.\gradlew.bat spotlessApply` formats Kotlin and Gradle Kotlin files.
- `.\gradlew.bat :app:compileFossDebugKotlin` is a quick Kotlin compile check.
- `.\gradlew.bat :app:assembleFossDebug` is useful after resource, manifest, or
  packaging changes.
- `.\gradlew.bat :app:assembleRelease` matches the CI build path for both
  flavors and should be used for release, flavor, R8, ProGuard, or packaging
  changes when feasible.

Important build facts:

- Java toolchain: 25.
- Kotlin is configured through Gradle and the version catalog.
- `compileSdk = 37`, `targetSdk = 37`, `minSdk = 24`.
- `foss` is the default flavor. `market` adds Google/Firebase integrations and
  market-specific source.
- Version name and code are computed in `build-logic/src/main/kotlin/Projects.kt`
  from `baseVersionName` and git history, so builds expect a real git checkout.
- There are currently no dedicated unit-test dependencies configured. If tests
  are introduced, put JVM tests under `src/test` and instrumented tests under
  `src/androidTest`.

For docs-only changes, a Gradle build is usually unnecessary. For source
changes, run the narrowest relevant command plus `spotlessCheck` when practical.

## Code Style

- Follow `.editorconfig`: UTF-8, 2-space indentation, final newline, and no
  trailing whitespace.
- Kotlin and `.gradle.kts` formatting is enforced by Spotless with ktlint.
- Trailing commas are disabled for Kotlin.
- Keep dependency versions in `gradle/libs.versions.toml`; avoid inline versions
  in module build files.
- Repositories are centralized in `settings.gradle.kts`; do not add project-level
  repositories.
- Prefer Kotlin for app code. Java is present for low-level parsers and platform
  stubs; keep it when extending those existing areas.

## App Architecture

Main packages under `com.absinthe.libchecker`:

- `LibCheckerApp.kt` initializes hidden API bypasses, Timber, telemetry, LCRules,
  repositories, Dynamic Colors, split-window rules, Coil app-icon loading, and
  cache cleanup.
- `features/home/` contains the main activity and `HomeViewModel`.
- `features/applist/` contains the app list and app/APK detail surfaces.
  `DetailViewModel` coordinates native, static, DEX, component, permission,
  metadata, signature, and Harmony ability analysis.
- `features/snapshot/` and `features/album/` cover snapshots, diffs, backups,
  comparisons, and package tracking.
- `features/statistics/` and `features/chart/` build library reference and chart
  views from collected app data.
- `database/` contains `LCDatabase`, `LCDao`, `LCRepository`, Room entities,
  migrations, and backup helpers.
- `data/app/` is the installed-app data source and package-change flow.
- `api/` holds Retrofit/OkHttp requests and endpoint switching.
- `services/` contains `ShootService`, `WorkerService`, and their AIDL contracts.
- `ui/base/`, `ui/app/`, `ui/adapter/`, and `view/` hold shared UI base classes,
  custom views, drawables, spans, and RecyclerView helpers.
- `utils/` contains APK/APKS/XAPK parsing, manifest readers, DEX helpers, ELF
  parsing, package utilities, OS/version compatibility helpers, preferences,
  logging trees, and extension functions.
- `compat/` wraps platform/API-level differences. Check here before adding new
  direct SDK-version branches.

Prefer adding code near the feature or utility that already owns the behavior.
Avoid broad refactors while fixing a narrow issue.

## UI Patterns

- The app uses XML layouts and ViewBinding, not Jetpack Compose UI.
- Activities generally extend `BaseActivity<VB>` and fragments extend
  `BaseFragment<VB>` or related base classes. Implement `inflateBinding` through
  the existing `IBinding` pattern.
- `BaseActivity` handles theme application, locale wrapping, edge-to-edge setup,
  and common inset padding. Reuse existing inset extension helpers.
- `BaseFragment` owns a nullable binding and clears it in `onDestroyView`; do not
  keep view references past the fragment view lifecycle.
- Keep UI work on the main thread and heavy parsing/database work on
  `Dispatchers.IO` or `Dispatchers.Default`.
- Prefer existing adapter, node/provider, and custom view patterns in the target
  feature before introducing new UI infrastructure.
- Use `Timber` for logging instead of Android `Log`.

## Data, Parsing, And Rules

- Room app data lives in `LCDatabase`. When changing entities or DAO schema:
  bump the database version, add a migration or auto-migration, and update
  `app/schemas/`.
- `LCRepository` is the main app database repository. `Repositories` owns app-wide
  repository/database access and rules database file cleanup.
- Library matching is provided by the external LCRules/rules bundle dependency.
  Avoid duplicating rule matching logic in UI code.
- Package analysis must handle installed apps, split APKs, APKS/XAPK archives,
  external APK files, Harmony/HAP metadata, missing icons/labels, corrupted
  archives, and OEM/API-level behavior differences.
- Use existing helpers such as `PackageUtils`, `PackageManagerCompat`,
  `PackageInfoExtensions`, `utils/apk`, `utils/manifest`, `utils/dex`, and
  `utils/elf` before adding another parser.
- Never block the main thread with package scanning, zip reads, DEX parsing, ELF
  parsing, database writes, or network calls.

## Flavors, Permissions, And Release Behavior

- Keep `foss` free of market-only Google/Firebase behavior. The FOSS source set
  contains stubs for APIs that the market flavor implements differently.
- `market` includes Firebase/Crashlytics and Google services dependencies.
- Manifest changes can affect exported activities, deep links, FileProvider,
  Shizuku provider authorities, package visibility, and foreground services.
  Review `app/src/main/AndroidManifest.xml` plus flavor/debug manifests.
- Be conservative with permissions, especially `QUERY_ALL_PACKAGES`, notification,
  foreground service, package installer, file URI, and Shizuku-related behavior.
- Release builds use R8/resource shrinking and custom AAPT2 resource optimization.
  Update `app/proguard-rules.pro` when adding reflection, generated binding entry
  points, Javascript interfaces, Parcelable creators, or hidden/private API access.

## Resources And Localization

- Add new user-facing strings to `app/src/main/res/values/strings.xml`.
- Use `app/src/main/res/values/untranslatable.xml` only for strings that should
  not go through Crowdin.
- Do not hand-update every translated `values-*` file unless the task explicitly
  asks for translations; Crowdin handles synchronization.
- Follow existing resource naming: `activity_*`, `fragment_*`, `item_*`,
  `layout_*`, `ic_*`, and `bg_*`.
- Prefer existing theme colors, dimensions, and styles instead of hardcoded
  colors or sizes in layouts.

## Dependency Changes

- Add or update libraries in `gradle/libs.versions.toml` first.
- Wire dependencies through `app/build.gradle.kts` using catalog aliases.
- Put flavor-only dependencies in the matching configuration, for example
  `marketImplementation` or `marketCompileOnly`.
- Keep KSP arguments in sync with processors. Room schemas currently write to
  `app/schemas`.

## Suggested Agent Workflow

1. Start with `git status --short` and inspect the smallest relevant area with
   `rg` or `rg --files`.
2. Read the existing feature code before editing; this project has many local
   patterns that are cheaper to reuse than replace.
3. Keep edits focused and avoid generated/build output churn.
4. After Kotlin or Gradle changes, run `spotlessApply` if needed, then
   `spotlessCheck`.
5. Run the narrowest compile/build task that covers the files touched.
6. If a validation command is skipped, state why in the final response.

