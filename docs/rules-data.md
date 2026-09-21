# Rule data and reader updates

Base rules are owned by LibChecker-Rules. App scanning/native evidence policy is
unchanged. Chart SVG policy and SDK-details distribution remain separate.

The offline baseline belongs exclusively to LibChecker-Rules-Bundle:
`assets/lcrules/v5/{rules.db,metadata.json}` inside the published AAR. App has no
independent database asset, ZIP or data-update workflow. The baseline is pinned
to Rules commit `e48a42108e4ec9caba2d7d808e8731742a01dac7`, dataVersion 45; its
producer Android ZIP SHA-256 is
`c91d36b9a7362a5b93c4707fc29a4ac20ecff13e889f29c4e75d53d35e924763`.
The fixed Bundle commit dependency binds reader code and baseline data. Program
versions remain independent of dataVersion.

The data updater and automatic PR workflow now belong to Bundle. Updating the
App baseline means updating its fixed Bundle dependency in the version catalog,
never generating another App database asset. Runtime cloud updates remain separate.
This App PR does not publish rules or the App.

The reader is resolved from JitPack through `implementation(libs.lc.rules)`:
`com.github.LibChecker:LibChecker-Rules-Bundle:ccaf38ac60`, built from merged
Bundle commit `ccaf38ac60f0f53a584da9b5da91fdb501ed0958` (PR #51).
The version catalog is the sole dependency version source; no vendored AAR or
local override is used. JVM archive tests read the same published dependency's
bundled data through a test-only Gradle configuration, without adding an App DB asset.

## Installation and restart

The v5 manifest is fetched from the selected repository's `rules-data`
branch. Missing or failed manifests never fall back to v4; installed v5 rules
remain usable offline. Downloads
are limited to 32 MiB and checked against immutable path, size and SHA-256 before
extraction. Extraction bounds path names, entry count, entry sizes and total size.
Only `rules.db` and `metadata.json` are accepted. Metadata identity and SQLite
integrity/schema/count/regex are checked before publishing an AtomicFile pointer. Original ZIP
bytes are retained and installed files are checked against them when reopened.

The current process keeps its reader until restart. `LCRules.init(context)`
independently prepares, verifies, repairs and opens the AAR baseline. App does not
provide a database file to initialize it. App's existing store manages only cloud
current/previous archives; validated candidates strictly newer than the baseline
may be selected with the explicit `activateDatabase(file)` update API. Failure
keeps the baseline open, and equal versions always prefer it. `getMetadata()` and
`getDatabaseFile()` describe the selected data. No v4 fallback remains.

Before activating a download, App prunes unused download directories while no
reader uses that store. Bundle's separately owned baseline directory is untouched.
Old App baseline copies and downloads at or below the AAR version are obsolete;
valid newer current/previous archives remain available for fallback.
After v5 opens successfully, obsolete v4 DBs and numeric version markers are removed
from their dedicated old paths. Other application data remains untouched. Historical
markers never participate in version selection, even if numerically higher.
A forward dataVersion is required to roll back content. App versions come only from
the selected v5 manifest. AAR and App have no v4 database initialization or download.

Android carries matching SQLite and minimal metadata only. Its nine columns are
`_id`, `name`, `label`, `type`, `iconIndex`, `isRegexRule`, `regexName`, `priority`, and nullable `labelEn`.
UUID and full library data remain in canonical/portable rules and cloud descriptions;
App receives the UUID from the existing cloud detail response. Description requests
use the existing cloud JSON paths, current GitHub/GitLab selection and locale/error
handling. Library icons use Bundle drawable/IconResMap in every surface; unknown
indexes use its placeholder. There is no local description or rule SVG path/cache.
Independent chart SVG support is unchanged. A new rule reusing a drawable can ship
as data; a new Android icon requires a Bundle resource release and App dependency
update. Rules' pinned XML sources and index generate Bundle resources separately.

Rule labels use the existing persisted `ruleLanguage` detail-popup preference.
An unset preference defaults to English; existing saved values are retained. Only
`zh` language tags select Chinese; all other languages select `labelEn`, falling
back to `label` when no English label is stored. System locale is irrelevant.
Language changes refresh loaded detail rows/native chip caches, recent visits,
reference display rows and snapshot diff content without rescanning APKs.
Programmatic popup fallback does not overwrite the preference; user selection does.

The unpublished full-content preview 45 was replaced locally by DB-only 45 with
refreshed checksums. This one-time bootstrap did not change normal updater rules:
a changed published release still requires a higher dataVersion.

SDK definition `index_path` and `entries_field` fetch fixed indexes. App compares
captured values locally using `expected_field`, then expands `items_field` and
maps outputs. Fixed indexes are bounded to 1 MiB. When an active definition still
uses a captured-value URL template, compatible clients fetch the corresponding
`sdk-details/candidates/<sdk_id>/definition.json` and validate it before probing.
Captured-value URLs are never requested. This allows staged fixed-index definitions
to work without replacing the definitions consumed by older clients.

SDK details use a dedicated 8 MiB OkHttp disk cache under `cacheDir`, honoring the
selected source's HTTP freshness and ETag headers. Response bodies are streamed
with decoded-size limits and parsed off the main thread; matching also runs off
the main thread. Only the latest successful package probe is retained in memory;
changes to probe definitions or source paths, sizes, modification times and
readability invalidate it. Failed reads are not cached.
The library dialog warms the catalog cache alongside its description request.
After definition validation, fixed indexes download concurrently with the local
probe; lookups still apply in definition order. Duplicate index paths share one
request per query, and unused downloads are cancelled when probing finds no evidence.

## Local verification boundaries

Run market R8 checks with the upload task explicitly excluded:

```sh
./gradlew :app:minifyMarketReleaseWithR8 -x :app:uploadCrashlyticsMappingFileMarketRelease
```

The existing market R8 task graph includes Crashlytics mapping upload. A previous
local migration check executed that task and it returned successfully; standard
logs contain no remote reception receipt. Do not infer offline-only behavior from
the word `minify`, and do not alter production upload configuration to run checks.

InstalledRulesBundleTest validates the AAR-owned database and metadata, SQLite, matching,
drawable lookup and absence of local details/SVG on Android. Gradle connected-test cleanup can remove the target
debug app. Prefer building the test APK, installing both APKs with `adb install -r`,
then `adb shell am instrument` for repeat checks; retain the debug app and its data.
Visible UI checks remain separate from JVM, packaging and instrumentation evidence.

## DB-only bootstrap evidence

FOSS release APK comparison for this migration (same build environment):

| Build | Bytes |
| --- | ---: |
| Pre-migration baseline | 4,707,407 |
| Unpublished full-content v5 preview | 5,363,932 |
| DB-only v5 dataVersion 45 with legacy reader fallback | 4,832,868 |
| V5-only with App-owned baseline | 4,762,690 |
| AAR-owned baseline before compact columns | 4,762,834 |
| Compact AAR baseline before bilingual labels | 4,721,498 |
| Bilingual vendored AAR baseline, dataVersion 45 | 4,728,230 |
| Published JitPack dependency, dataVersion 45 | 4,728,318 |

The unique bundled database now comes from the AAR. App's download store retains
original archives only for actual cloud updates, for checksum verification and
repair; the bundled baseline needs no duplicate archive. On the existing Pixel 9a,
the earlier DB-only transition removed the 18,796 KiB full-content preview without
clearing app data. The subsequent AAR transition removes App's old baseline copy.

Other rendering surfaces are compiled but not individually
exercised on device. The device has no old v4 directories to remove; precise cleanup
and a high old version marker are covered in JVM temporary-directory tests.

Earlier compact-baseline validation: FOSS `spotlessCheck`, 16 focused JVM tests and debug/test/release
packaging passed. Two manual instrumented tests verified the AAR baseline,
drawable/metadata and manifest-404 failure without changing installed rules.
APK inspection found exactly one database asset, matching the then-locked AAR byte for
byte. After in-place upgrade, App's old download directory was empty (4 KiB), while
Bundle's compact baseline directory occupied 191 KiB. Debug app data remained intact and
AndroMeld confirmed visible launch and the LocalSend Flutter/DataStore drawables
and Flutter cloud detail flow after installing the compact baseline.
The vendored bilingual APK was 34,604 bytes smaller than the pre-compaction AAR
baseline and 20,823 bytes larger than the original app; bilingual labels add
6,732 bytes over the compact baseline. Compression and FOSS obfuscation policy
were unchanged.

Bilingual-label validation: `spotlessCheck`, 11 focused JVM tests, FOSS debug/test/
release packaging and four manually invoked device tests passed. Device tests
cover unset-English behavior under Chinese system locale, persisted language
selection, differing Xinge Push labels, loaded-row/native-cache refresh and
programmatic popup fallback versus user selection. AndroMeld verified IT Home's
Huawei Push cloud popup switches and remembers Chinese, then restored English.
That rule has identical Chinese/English labels, so this smoke does not prove a
visible row-text difference; other refresh surfaces were not individually exercised.
The debug app remains installed with its data preserved. The manual smoke leaves
the rule language set to English; its pre-smoke raw preference was not recorded.


Published-dependency validation: JitPack's `ccaf38ac60` build succeeded. App resolved
its AAR from Maven, with SHA-256
`a8cb9632dcc2583eb4ec1c0ad9371aa52303cd46e71ac26b9605ba42e987a2da`, identical
to the previously validated AAR. After cleaning App's stale incremental outputs,
`spotlessCheck`, seven `RuleBundleStoreTest` cases, `assembleFossDebug` and
`assembleFossRelease` passed; Gradle stored the configuration cache successfully.
The release APK is 4,728,318 bytes and contains exactly one rule database, with
DB and metadata byte-identical to the published AAR. Device/UI checks were not
repeated for this dependency-only change; the installed debug app/data remain intact.
