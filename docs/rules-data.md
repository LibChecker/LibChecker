# Rule data and reader updates

Base rules are owned by LibChecker-Rules. App scanning/native evidence policy is
unchanged. Chart SVG policy and SDK-details distribution remain separate.

The offline baseline belongs exclusively to LibChecker-Rules-Bundle:
`assets/lcrules/v5/{rules.db,metadata.json}` inside the locked AAR. App has no
independent database asset, ZIP or data-update workflow. The baseline is pinned
to Rules commit `7b0a6f92806de7b6c7cf578049b28b555564c223`, dataVersion 45; its
producer Android ZIP SHA-256 is
`eda664bd0956723db367eb4696e1d5cf64142ff403e3ebd93cf04b97dc07103f`.
The AAR SHA lock binds both reader code and baseline data. Program versions remain
independent of dataVersion.

The data updater and automatic PR workflow now belong to Bundle. Updating the
App baseline means updating its verified AAR (or a future published dependency),
never generating another App database asset. Runtime cloud updates remain separate.
No rules, reader or App publication occurs in this migration.

The reader is temporarily vendored as a verified real AAR; source commit, license,
rebuild commands and release gate are in `third-party/rules-reader/README.md`.
Default builds verify its SHA-256. No unpublished Maven version is assumed.

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

Android carries matching SQLite and minimal metadata only. Its eight columns are
`_id`, `name`, `label`, `type`, `iconIndex`, `isRegexRule`, `regexName`, and `priority`.
UUID and full library data remain in canonical/portable rules and cloud descriptions;
App receives the UUID from the existing cloud detail response. Description requests
use the existing cloud JSON paths, current GitHub/GitLab selection and locale/error
handling. Library icons use Bundle drawable/IconResMap in every surface; unknown
indexes use its placeholder. There is no local description or rule SVG path/cache.
Independent chart SVG support is unchanged. A new rule reusing a drawable can ship
as data; a new Android icon requires a Bundle resource release and App dependency
update. Rules' pinned XML sources and index generate Bundle resources separately.

The unpublished full-content preview 45 was replaced locally by DB-only 45 with
refreshed checksums. This one-time bootstrap did not change normal updater rules:
a changed published release still requires a higher dataVersion.

SDK definition `index_path` and `entries_field` fetch fixed indexes. App compares
captured values locally using `expected_field`, then expands `items_field` and
maps outputs. Fixed indexes are bounded to 1 MiB. Captured-value URL templates
are rejected. Producer candidate definitions/indexes are regression fixtures;
remote activation must follow deployment of this client support.

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
| Final compact AAR baseline, dataVersion 45 | 4,721,498 |

The unique bundled database now comes from the AAR. App's download store retains
original archives only for actual cloud updates, for checksum verification and
repair; the bundled baseline needs no duplicate archive. On the existing Pixel 9a,
the earlier DB-only transition removed the 18,796 KiB full-content preview without
clearing app data. The subsequent AAR transition removes App's old baseline copy.

The independent SDK version card remains unavailable because candidate definitions
are not published. Other rendering surfaces are compiled but not individually
exercised on device. The device has no old v4 directories to remove; precise cleanup
and a high old version marker are covered in JVM temporary-directory tests.

Final validation: FOSS `spotlessCheck`, 16 focused JVM tests and debug/test/release
packaging passed. Two manual instrumented tests verified the AAR baseline,
drawable/metadata and manifest-404 failure without changing installed rules.
APK inspection found exactly one database asset, matching the locked AAR byte for
byte. After in-place upgrade, App's old download directory was empty (4 KiB), while
Bundle's compact baseline directory occupied 191 KiB. Debug app data remained intact and
AndroMeld confirmed visible launch and the LocalSend Flutter/DataStore drawables
and Flutter cloud detail flow after installing the compact baseline.
The final APK is 41,336 bytes smaller than the pre-compaction AAR baseline and
14,091 bytes larger than the original app. Only one final compact-AAR build was
used for this comparison; compression and FOSS obfuscation policy were unchanged.
