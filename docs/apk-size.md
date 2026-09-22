# APK size validation

Compare release APKs built from the same commit, flavor, signing configuration,
and toolchain. Save the baseline outside the repository before editing:

```sh
./gradlew :app:assembleFossRelease :app:assembleMarketRelease \
  -x :app:uploadCrashlyticsMappingFileMarketRelease
```

Check that resource shrinking preserves resource IDs, configurations, values,
and bundled data with:

```sh
python3 .github/scripts/verify_apk_resources.py before.apk after.apk \
  --aapt2 "$ANDROID_HOME/build-tools/37.0.0/aapt2" \
  --allow-redundant-rasters
```

For lossless image changes, add `--ffmpeg /path/to/ffmpeg`. This checks decoded
dimensions and every RGBA byte, including transparent pixels. Encode with
`cwebp -lossless -z 9 -exact -metadata all`; preserve color metadata and resource
density. The checker excludes DEX-dependent baseline profiles and normalizes
the generated Crashlytics mapping ID.

`ResoptPlugin` removes concrete-density PNG alternatives only when an otherwise
unqualified `anydpi` XML resource covers the app's minimum API level. For example,
the Rules AAR contains PNG fallbacks for API 21–23 alongside `anydpi-v24` vectors;
the app's minSdk 24 always selects those vectors. Keep density-specific resources
with other qualifiers and resources required below a vector's API level. The
verifier checks this independently from AAPT's resource dump and compares the
manifest and every surviving XML file byte-for-byte.

Test the API 23/24 boundary with resources actually linked for minSdk 23. AAPT
can remove redundant version qualifiers when linking for minSdk 24, so passing
23 to the filter on an already-linked API 24 table is not a valid fallback test.

The resource-table conversion must preserve the original manifest and binary XML
entries. A binary/proto/binary conversion can reinterpret XML string escaping.
The plugin therefore copies only the converted resource table into the original
archive and removes proven unreachable raster files. Its AAPT/Protobuf build
dependencies track AGP; they are not packaged into the application.

## Keep-rule boundaries

- `android-defaults.keep` mirrors AGP 9.4.1 defaults except that MPAndroidChart
  properties and enums are not retained for reflection. Resync it when upgrading AGP.
- `androidx-annotations.keep` mirrors AndroidX annotation 1.10.0 rules except
  the class-wide `@Keep` on MPAndroidChart classes. Resync it when upgrading
  AndroidX annotation. Other annotation behavior is unchanged.
- Market's `play-services-basement.keep` similarly scopes its duplicate chart
  retention. Dependency filters match exact versions so an upgraded dependency
  retains its new consumer rules until the local copy is reviewed.
- MPAndroidChart 4 charts are constructed and configured directly; its animation
  uses `ValueAnimator`. Recheck these assumptions when upgrading the library or
  introducing XML inflation or property-name animation.
- Keep Fragment no-argument constructors: `FragmentFactory` invokes them by
  reflection, including class-based transactions. A successful build can otherwise
  hide missing settings screens and their resources.
- FOSS deliberately retains class names for reproducible-build diagnostics.

Resource equivalence does not validate R8 behavior. Smoke-test a minified
`fossBenchmark` build on the device, including charts, chart detail navigation,
and settings. It updates the debug package in place; reinstall `fossDebug` after
validation and preserve app data. Resource packaging changes also need a release
APK check: benchmark retains resource names for UI automation and does not run
the release-only resource optimizer. Use a temporary release validation build
with the debug application ID/signing key, leaving the regular release artifact
and the installed release app intact.
