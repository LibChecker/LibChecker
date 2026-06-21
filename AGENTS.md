# AGENTS.md

Root instructions for coding agents in this repository. Keep this file short,
operational, and focused on decisions that are easy to get wrong.

## Core commands

Use the Gradle wrapper from the repository root. On macOS/Linux use
`./gradlew`; on Windows use `.\gradlew.bat`. CI uses `./gradlew`.

- Format check: `./gradlew spotlessCheck`
- Apply Kotlin/Gradle formatting: `./gradlew spotlessApply`
- Fast Kotlin compile: `./gradlew :app:compileFossDebugKotlin`
- Build a runnable debug APK: `./gradlew :app:assembleFossDebug`
- Install the default debug flavor on a connected device:
  `./gradlew :app:installFossDebug`
- Manual device launch after installing debug should target
  `com.absinthe.libchecker.debug`; `com.absinthe.libchecker` may be a separate
  release install used for snapshot export/import checks.
- Release/R8/package validation: `./gradlew :app:assembleRelease`
- Market R8 rule check when full signing is blocked:
  `./gradlew :app:minifyMarketReleaseWithR8`
- Device UI validation: prefer AndroMeld MCP Phone Screen sessions for visible
  launch, navigation, and UI-state checks. Use Gradle/adb for install and
  package-state operations only when needed.

For docs-only changes, a Gradle build is usually unnecessary. For source
changes, run the narrowest command that covers the touched files plus
`spotlessCheck` when practical. For resource, manifest, packaging, R8, flavor,
or release behavior changes, run the matching assemble/minify task.

## Build facts

- Java toolchain: 25.
- SDK levels are configured in `build-logic/src/main/kotlin/Projects.kt`
  (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 24`).
- `foss` is the default flavor. `market` adds Google/Firebase integrations.
- There is no dedicated unit-test gate today. If tests are added, put JVM tests
  under `src/test` and instrumented tests under `src/androidTest`.
- Version name/code come from `baseVersionName` plus git state in
  `build-logic/src/main/kotlin/Projects.kt`; build from a real git checkout.
- CI runs `./gradlew --non-interactive spotlessCheck` and
  `./gradlew --non-interactive app:assembleRelease`.

## Module boundaries

- `:app` is the Android application. Put product behavior here.
- `:hidden-api` is compile-only hidden platform API stubs and Rikka Refine
  annotations. Never put runtime app logic here.
- `build-logic/` owns shared Gradle conventions and custom plugins.

Important `:app` boundaries:

- `features/*` owns user-facing feature flows. Add code near the feature that
  owns the behavior.
- `domain/app/` owns app-list use cases and repository/factory interfaces.
  Keep package-list synchronization rules here instead of in UI controllers.
- `data/app/` adapts Android package APIs, Room repositories, and local
  package-change sources to the `domain/app/` interfaces.
- `domain/statistics/` owns statistics/reference computation rules. Keep
  package scanning and rule-matching loops out of fragments and ViewModels.
- `domain/snapshot/` owns snapshot models, archive, capture, and diff seams;
  keep package-to-snapshot conversion and diff rules out of UI controllers.
- `data/snapshot/` adapts Android, protobuf archive format, and local snapshot
  storage to `domain/snapshot/` interfaces.
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

- Room schema changes require a database version bump, migration or
  auto-migration, and updated `app/schemas/`.
- UI controllers should not call `Repositories.lcRepository` directly for new
  or refactored paths; route persistence through ViewModels and domain use
  cases/repositories.
- Avoid package-manager, archive, or freeze-state lookups in RecyclerView/view
  binding; precompute through ViewModels/use cases on a background thread.
- Heavy package scanning, zip reads, DEX parsing, ELF parsing, database writes,
  and network calls must run off the main thread.
- Package analysis must keep working for installed apps, APK, split APK, APKS,
  XAPK, HAP, missing icons/labels, corrupted archives, and OEM/API differences.
- Keep `foss` free of market-only Google/Firebase behavior.
- Review manifests carefully when changing exported activities, deep links,
  FileProvider, Shizuku provider authorities, package visibility, foreground
  services, or sensitive permissions.
- Update keep rules when adding reflection, generated binding entry points,
  JavaScript interfaces, Parcelable creators, or hidden/private API access.

## Environment gotchas

- If Gradle/Kotlin validation fails because local writes under `~/.gradle`,
  `~/.android`, file watchers, or the Kotlin daemon are blocked, retry with:
  `GRADLE_USER_HOME=/private/tmp/libchecker-gradle-home`
  `ANDROID_USER_HOME=/private/tmp/libchecker-android-home`
  `-Dorg.gradle.vfs.watch=false`
  `-Dkotlin.compiler.execution.strategy=in-process`
- Do not treat every Gradle deprecation trace as repo-owned. Recent AGP traces
  such as `VariantDependenciesBuilder.createTestComponents` were upstream/plugin
  noise, not a reason to rewrite project dependency access.
- Keep `TYPESAFE_PROJECT_ACCESSORS` while `app/build.gradle.kts` uses
  `projects.hiddenApi`.
- Release signing may fail locally because debug/release keystore creation is
  blocked. For R8 rule proof, inspect generated
  `app/build/outputs/mapping/*/configuration.txt` and `mapping.txt`.

## NEVER

- Never revert or overwrite user changes unless explicitly asked.
- Never commit generated build output, `.gradle/`, `.kotlin/`, `app/build/`,
  `app/foss/`, or `app/market/`.
- Never move runtime logic into `:hidden-api`.
- Never add Google/Firebase behavior to `foss`.
- Never hand-update all translated `values-*` resources unless explicitly asked;
  Crowdin handles synchronization.
- Never block the main thread with package parsing, database, zip, DEX, ELF, or
  network work.
- Never add inline dependency versions or project-level repositories.
- Never broaden a narrow bug fix into an unrelated refactor.
- Never remove `projects.hiddenApi` or `TYPESAFE_PROJECT_ACCESSORS` just because
  of an upstream Gradle/AGP warning.
- Never use destructive git commands such as `git reset --hard`, `git clean`, or
  checkout-based reverts unless the user explicitly requests them.

## Agent workflow

1. Start with `git status --short`.
2. Inspect the smallest relevant area with `rg` or `rg --files`.
3. Read existing local patterns before editing.
4. For refactors, prioritize high-traffic user-facing flows before low-frequency
   tooling. Keep edits focused and avoid generated/build-output churn.
5. Run `spotlessApply` only when formatting needs fixing.
6. Run the narrowest relevant validation command, then report exactly what
   passed, failed, or was skipped.
7. Before committing code, consider `AGENTS.md` only for durable, recurring
   rules. Keep it compact: merge with existing bullets, replace stale guidance,
   or delete obsolete notes before appending. Put one-off decisions and
   low-frequency background in commit messages, issues, or Skills instead.

## Compact instructions

If context is compacted, preserve these facts:

- Current user request and any exact issue/PR/comment/commit links.
- Files already read and files changed.
- Commands run and their pass/fail/blocker results.
- Any user constraints, especially flavor, release, R8, accessibility, or
  copyability requirements.
- Current git status and whether changes are user-owned or agent-owned.
- Environment workaround state, including temp Gradle/Android homes and Kotlin
  in-process/VFS flags.
- Any unresolved decision that must not be guessed after compaction.
