# AGENTS.md

Root instructions for coding agents in this repository. Keep this file short,
operational, and focused on decisions that are easy to get wrong.

## Task scope and follow-through

- Investigation, review, and explicit planning requests produce findings or a
  plan. Requests to fix, implement, or apply an approved plan authorize that
  work; continue through relevant verification without asking again.
- Make reasonable assumptions for reversible implementation details. Ask only
  when missing information materially changes scope or correctness, or an
  action needs authorization not already provided in the current task.
- Follow explicit user instructions over skill guidelines, within runtime
  permissions. If a skill blocks progress, name its file and quote the blocking
  instruction instead of silently adding an approval step.
- Report the result, relevant verification, and remaining blockers concisely.
  Distinguish build/test evidence from device-visible behavior.

## Core commands

Use the Gradle wrapper from the repository root. On macOS/Linux use
`./gradlew`; on Windows use `.\gradlew.bat`. CI uses `./gradlew`.
For device commands on PowerShell, set `$env:ANDROID_SERIAL = '<serial>'`
before invoking the wrapper instead of using the POSIX inline assignment.

- Format check: `./gradlew spotlessCheck`
- Apply Kotlin/Gradle formatting: `./gradlew spotlessApply`
- Fast Kotlin compile: `./gradlew :app:compileFossDebugKotlin`
- JVM tests: `./gradlew :app:testFossDebugUnitTest`
  Add `--tests '<fully-qualified-test-class>'` for a focused run.
- Build a runnable debug APK: `./gradlew :app:assembleFossDebug`
- Install the default debug flavor on a connected device:
  `./gradlew :app:installFossDebug`
- Manual device launch after installing debug should target
  `com.absinthe.libchecker.debug`; `com.absinthe.libchecker` may be a separate
  release install used for snapshot export/import checks.
- Release/R8/package validation: `./gradlew :app:assembleRelease`
- Market R8 rule check when full signing is blocked:
  `./gradlew :app:minifyMarketReleaseWithR8`
- Jetpack Macrobenchmark targeted smoke:
  `ANDROID_SERIAL=<serial> ./gradlew :macrobenchmark:connectedFossBenchmarkAndroidTest --no-configuration-cache`
  Add `-Pandroid.testInstrumentationRunnerArguments.class=<BenchmarkClass#method>`
  to run one startup, app-list, or detail scenario.
- App instrumented test:
  `ANDROID_SERIAL=<serial> ./gradlew :app:connectedFossDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<TestClass#method>`
- Device UI validation: prefer AndroMeld MCP Phone Screen sessions for visible
  launch, navigation, and UI-state checks. Use Gradle/adb for install and
  package-state operations only when needed. For performance/detail checks, use
  real complex packages instead of trivial sample apps when available.
- For snapshot checks, prefer importing an existing backup from the device
  `Download` directory before exporting a new snapshot.
- Files generated on test devices must be created only under `/data/local/tmp`
  (including screenshots, recordings, logs, traces, and snapshot exports).
  Never generate files in `/sdcard`, `Download`, or any other device directory.

For docs-only changes, a Gradle build is usually unnecessary. For source
changes, run the narrowest command that covers the touched files plus
`spotlessCheck` when practical. For resource, manifest, packaging, R8, flavor,
or release behavior changes, run the matching assemble/minify task.
Use meaningful regression checks for changed behavior; avoid tests that merely
repeat the implementation. Once relevant checks pass, broaden or repeat them
only for new changes, failures, or unresolved concerns.

## Build facts

- Java toolchain: 25.
- SDK levels are configured in `build-logic/src/main/kotlin/Projects.kt`
  (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 24`).
- `foss` is the default flavor. `market` adds Google/Firebase integrations.
- Put JVM tests under `app/src/test` and device tests under
  `app/src/androidTest`. CI does not currently run either suite, so run the
  narrowest relevant tests locally.
- Version name/code come from `baseVersionName` plus git state in
  `build-logic/src/main/kotlin/Projects.kt`; build from a real git checkout.
- CI runs workflow-script tests, `spotlessCheck`, and separate
  `assembleFossRelease`/`assembleMarketRelease` builds.

## Module boundaries

- `:app` is the Android application. Put product behavior here.
- `:compat` owns in-repo compatibility shims, shaded/stubbed third-party API
  surfaces, and source-level workarounds that must keep their original package
  names. Do not put product behavior here.
- `:hidden-api` is compile-only hidden platform API stubs and Rikka Refine
  annotations. Never put runtime app logic here.
- `:macrobenchmark` owns Jetpack Macrobenchmark tests for release-like app
  performance checks. Keep scenarios focused on stable, high-value user flows.
- `build-logic/` owns shared Gradle conventions and custom plugins.

Important `:app` boundaries:

- User-facing flows live in focused `domain/<product>/` vertical slices. Keep
  presentation state, UI, models, use cases, and repository contracts with the
  owning product; do not recreate the removed `features/*` structure.
- `domain/*` owns product policy and repository contracts. Matching `data/*`
  packages adapt Android APIs, persistence, and remote sources.
- `domain/app/` owns app-list use cases and repository/factory interfaces.
  Keep package-list synchronization rules here instead of in UI controllers.
  Put feature-specific app-domain use cases and display models in focused
  subpackages instead of growing the root package by default.
- `domain/statistics/` owns statistics/reference computation rules. Keep
  package scanning, package-info lookups, and rule-matching loops out of
  fragments, ViewModels, and chart data sources. Model built-in and external
  charts through the same definition catalog; built-ins use drawable icon
  keys, while external rule icons use validated SVG files.
- `domain/snapshot/` owns snapshot models, archive, capture, and diff seams;
  keep package-to-snapshot conversion and diff rules out of UI controllers and
  services.
- `domain/app/detail/insight/` owns the bounded generic interpreter for remote
  SDK details. Keep SDK-specific UUIDs, archive paths, prefixes, artifact
  coordinates, and fingerprint data in LibChecker-Rules rather than hardcoding
  them in the client. Fetch fingerprint indexes by stable paths and match them
  locally; never put locally captured fingerprints in request URLs or logs.
- `ui/base/` owns shared screen/controller behavior; `view/app/` owns reusable
  rendering widgets. Reuse `BaseListControllerFragment`, `ListScreenChrome`,
  `BaseBottomSheetViewDialogFragment`, `BottomSheetScaffoldView`,
  `BindOnlyAdapter`, and `addSpacingDecoration` before adding screen-local
  equivalents.
- Shared views own rendering, accessibility metadata, and animation only. Pass
  prepared domain/display data in; do not import `data/*` from views.
- `compat/` wraps platform/API-level differences. Check here before adding new
  SDK-version branches.
- `utils/apk`, `utils/manifest`, `utils/dex`, `utils/elf`, `PackageUtils`,
  `PackageManagerCompat`, and `PackageInfoExtensions` own package parsing and
  package-manager helpers. Reuse them before adding another parser.
- `database/` owns Room entities, DAO, repository, migrations, schemas, and
  backup helpers.
- `app/src/foss/` and `app/src/market/` are flavor source sets. Keep matching
  APIs when touching flavor delegates.
- `app/src/main/res/values/strings.xml` is for user-facing strings.
  `values/untranslatable.xml` is only for strings that should not go through
  Crowdin.

## Style and naming

- Follow `.editorconfig`: UTF-8, 2-space indentation, final newline, no trailing
  whitespace.
- Kotlin and `.gradle.kts` formatting is enforced by Spotless/ktlint.
- Kotlin trailing commas are disabled.
- Keep dependency versions in `gradle/libs.versions.toml`; wire them through
  catalog aliases.
- Repositories are centralized in `settings.gradle.kts`; do not add module-level
  repositories.
- Prefer Kotlin for app code. Keep Java in existing Java-heavy parser/stub areas.
- Use XML layouts and ViewBinding, not Jetpack Compose UI.
- Activities/fragments should follow existing `BaseActivity<VB>`,
  `BaseFragment<VB>`, and `IBinding` patterns.
- Dependency injection uses Koin. Put app-wide bindings in `di/AppModule.kt`;
  inject ViewModels through Koin instead of default-constructing repositories,
  use cases, or platform adapters inside ViewModels.
- Use `Timber` instead of Android `Log`.
- Follow existing resource prefixes such as `activity_*`, `fragment_*`,
  `item_*`, `layout_*`, `ic_*`, and `bg_*`.

## Data, UI, and release constraints

- Keep full-width selectable or hoverable list rows edge-to-edge. Put horizontal
  page spacing in each row's content padding, not item margins or parent-list
  horizontal padding, so selector and hover feedback have no side gaps.
- Prefer flat display/render-state rows and reusable item views over
  one-class-per-row adapter or provider hierarchies.
- Room schema changes require a database version bump, migration or
  auto-migration, and updated `app/schemas/`.
- UI controllers should not call `Repositories.lcRepository` directly for new
  or refactored paths; route persistence through ViewModels and domain use
  cases/repositories.
- Avoid package-manager, archive, or freeze-state lookups in UI controllers or
  RecyclerView/view binding; precompute through ViewModels/use cases on a
  background thread.
- Avoid broad `PackageInfo` flag combinations for huge apps; prefer focused
  lookups that keep Binder payloads below transaction limits.
- Heavy package scanning, zip reads, DEX parsing, ELF parsing, database writes,
  and network calls must run off the main thread.
- Publish client support for a new SDK-details schema, reader, or capture type
  before publishing remote definitions that use it; older clients safely reject
  unknown definitions but cannot display their details.
- Package analysis must keep working for installed apps, APK, split APK, APKS,
  XAPK, HAP, missing icons/labels, corrupted archives, and OEM/API differences.
- Prefer `FileProvider` for sharing/exporting app files. Any legacy `file://`
  exposure must stay narrowly scoped and idempotent; new paths should not
  expand it.
- Keep `foss` free of market-only Google/Firebase behavior.
- Review manifests carefully when changing exported activities, deep links,
  FileProvider, Shizuku provider authorities, package visibility, foreground
  services, or sensitive permissions.
- When moving UI component packages, update manifest entries, direct intent
  refs, layout `tools:context`, and split/window embedding configs together.
- Update keep rules when adding reflection, generated binding entry points,
  JavaScript interfaces, Parcelable creators, or hidden/private API access.

## Build and device environment

- Use existing Gradle and Android homes. If validation is blocked by environment
  permissions, follow the runtime approval flow; do not change cache locations
  to bypass it. Any authorized temporary home must use a writable path for the
  current OS, with `ANDROID_USER_HOME` and the debug keystore kept stable across
  installs.
- Keep `TYPESAFE_PROJECT_ACCESSORS` while `app/build.gradle.kts` uses
  `projects.hiddenApi`; trace warnings to their owner before changing build
  configuration.
- For R8 rule validation, inspect generated
  `app/build/outputs/mapping/*/configuration.txt` and `mapping.txt`.
  R8 validation alone does not prove release signing or packaging succeeds.
- Ensure device freezer/background-management settings allow instrumentation
  packages to run during macrobenchmarks.

## NEVER

- Never revert or overwrite user changes, and never commit generated build
  output (`.gradle/`, `.kotlin/`, `app/build/`, `app/foss/`, or `app/market/`).
- Never hand-update all translated `values-*` resources unless explicitly asked;
  Crowdin handles synchronization.
- Never broaden a narrow bug fix into an unrelated refactor.
- Never use destructive git commands such as `git reset --hard`, `git clean`, or
  checkout-based reverts unless the user explicitly requests them.

## Agent workflow

1. Start with `git status --short`.
2. Inspect the smallest relevant area with `rg` or `rg --files`.
3. Read existing local patterns and trace affected callers before editing.
   Batch independent reads; use available subagents for independent work only
   when they save time or improve coverage. Keep dependent edits sequential.
4. For refactors, move one cohesive vertical slice at a time. Keep package
   moves mechanical and separate from behavior; avoid thin pass-through types
   and generated/build-output churn.
5. Run `spotlessApply` only when formatting needs fixing.
6. Run the narrowest relevant validation command. If adapters, view-state
   mapping, menus, navigation, visible strings, or performance-sensitive paths
   changed, add a focused AndroMeld smoke on an affected complex real-app flow
   when a device is available. Report exactly what passed, failed, or was
   skipped.
7. Before committing code, consider `AGENTS.md` only for durable, recurring
   rules. Keep it compact: merge with existing bullets, replace stale guidance,
   or delete obsolete notes before appending. Put one-off decisions and
   low-frequency background in commit messages, issues, or Skills instead.

## Compact instructions

If context is compacted, preserve these facts:

- Current request, exact links, and constraints such as flavor, release, R8,
  accessibility, or copyability requirements.
- Files read/changed, current git status, and change ownership.
- Commands run and their pass/fail/blocker results.
- Active build environment, including any temporary Gradle/Android homes and
  validation flags.
- Any unresolved decision that must not be guessed after compaction.
