# Transitional rules reader

`library-release.aar` is the actual, unpublished LibChecker-Rules-Bundle reader.
Source: https://github.com/LibChecker/LibChecker-Rules-Bundle
Local reviewed commit: `462bc6e0f980494159bb9ce9a851c1182952a51f` on `codex/rules-v5-reader`.
This commit is local to the coordinated migration and has not been pushed or released.
License: Apache-2.0, copied in `LICENSE`; upstream has no NOTICE file.
The AAR owns the only bundled v5 database and metadata under `assets/lcrules/v5/`,
plus drawable icons and their upstream attribution. `LCRules.init(Context)` prepares
and verifies its offline baseline; App supplies no initialization database.
`activateDatabase(File)` is reserved for explicit validated cloud updates.

Rebuild from that exact commit with JDK 17 and Android SDK 36:

```sh
./gradlew :library:clean :library:assembleRelease
shasum -a 256 library/build/outputs/aar/library-release.aar
```

The reproducible AAR SHA-256 is recorded in `SHA256` and checked by App `preBuild`.
For a local replacement pass `-PrulesBundleAar=/absolute/path/library-release.aar`
and `-PrulesBundleSha256=<verified-sha256>`.
Composite builds cannot bridge the current App Gradle 9.7 / Bundle AGP 8 toolchains.

Default App/CI builds use the checked-in verified AAR and require no unpublished
Maven coordinate. Before publishing the App, make this reader source commit
public and retain reproducible source access, or replace this directory with the
corresponding published reader dependency in a separate PR. No reader or App has
been published by this migration.

This reader consumes DB-only v5 packages. Library descriptions stay on cloud JSON;
icons and their color flags come from Bundle drawable/IconResMap. Unknown indexes
use a monochrome placeholder. The 174 XML sources and generated map are pinned to
Rules commit `7b0a6f92806de7b6c7cf578049b28b555564c223`; adding a new Android icon
requires a new Bundle resource release and App dependency update.

The compact dataVersion 45 baseline comes from Rules commit
`7b0a6f92806de7b6c7cf578049b28b555564c223`. Android keeps the original seven
matching columns plus priority; UUID and full metadata remain in cloud details
and canonical/portable data. Rule retains its original seven Parcelable fields.
