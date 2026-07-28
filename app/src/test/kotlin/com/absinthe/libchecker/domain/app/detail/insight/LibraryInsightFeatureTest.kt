package com.absinthe.libchecker.domain.app.detail.insight

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibraryInsightFeatureTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val validator = LibraryInsightDefinitionValidator()

  @Test
  fun `accepts the supported catalog and definition contract`() {
    assertTrue(validator.isValid(catalog()))
    assertTrue(validator.isValid(definition(), SDK_ID, LIBRARY_UUID))
  }

  @Test
  fun `accepts the bounded version 2 capture contract`() {
    assertTrue(
      validator.isValid(
        version2Definition(
          archivePath = ANDROIDX_VERSION_PATH,
          captureType = LibraryInsightDefinitionValidator.CAPTURE_SEMVER
        ),
        ANDROIDX_SDK_ID,
        ANDROIDX_LIBRARY_UUID
      )
    )
  }

  @Test
  fun `rejects catalog paths outside the versioned SDK details layout`() {
    val invalidPaths = listOf(
      "sdk-details/sdks/flutter/definition.json?raw=1",
      "sdk-details/sdks/flutter/definition.json#fragment",
      "sdk-details/sdks/%2e%2e/definition.json",
      "sdk-details/sdks/flutter/custom.json",
      "sdk-details/../secret.json"
    )

    invalidPaths.forEach { path ->
      assertFalse(
        path,
        validator.isValid(catalog(definitionPath = path))
      )
    }
  }

  @Test
  fun `probes matching evidence from split APKs`() = runBlocking {
    val packageInfo = packageInfoWithEngine(
      "\u0000$ENGINE_REVISION\u00003.22.1 (stable)\u0000"
    )

    val result = LibraryInsightProbeEngine().probe(packageInfo, definition())

    assertTrue(result.evidenceFound)
    assertEquals(listOf(ENGINE_REVISION), result.values["engine_revisions"])
    assertEquals(listOf("3.22.1"), result.values["dart_versions"])
  }

  @Test
  fun `probes a plain AndroidX semantic version from a split APK`() = runBlocking {
    val packageInfo = packageInfoWithEntries(
      splitEntries = mapOf(
        ANDROIDX_VERSION_PATH to "1.13.0-alpha01\n".toByteArray()
      )
    )

    val result = LibraryInsightProbeEngine().probe(
      packageInfo,
      version2Definition(
        archivePath = ANDROIDX_VERSION_PATH,
        captureType = LibraryInsightDefinitionValidator.CAPTURE_SEMVER
      )
    )

    assertTrue(result.evidenceFound)
    assertEquals(listOf("1.13.0-alpha01"), result.values["versions"])
  }

  @Test
  fun `does not capture a semantic version truncated by the reader limit`() = runBlocking {
    val packageInfo = packageInfoWithEntries(
      baseEntries = mapOf(
        ANDROIDX_VERSION_PATH to "1.13.0x\u0000".toByteArray()
      )
    )

    val result = LibraryInsightProbeEngine().probe(
      packageInfo,
      version2Definition(
        archivePath = ANDROIDX_VERSION_PATH,
        captureType = LibraryInsightDefinitionValidator.CAPTURE_SEMVER,
        maxBytesPerFile = "1.13.0".length.toLong()
      )
    )

    assertTrue(result.evidenceFound)
    assertTrue(result.values["versions"].isNullOrEmpty())
  }

  @Test
  fun `captures a semantic version ending exactly at EOF`() = runBlocking {
    val packageInfo = packageInfoWithEntries(
      baseEntries = mapOf(
        ANDROIDX_VERSION_PATH to "1.13.0".toByteArray()
      )
    )

    val result = LibraryInsightProbeEngine().probe(
      packageInfo,
      version2Definition(
        archivePath = ANDROIDX_VERSION_PATH,
        captureType = LibraryInsightDefinitionValidator.CAPTURE_SEMVER,
        maxBytesPerFile = "1.13.0".length.toLong()
      )
    )

    assertEquals(listOf("1.13.0"), result.values["versions"])
  }

  @Test
  fun `probes a literal-prefixed version from bounded DEX entries`() = runBlocking {
    val packageInfo = packageInfoWithEntries(
      baseEntries = mapOf(
        "classes.dex" to "\u0000AndroidXMedia3/1.10.1\u0000".toByteArray()
      )
    )

    val result = LibraryInsightProbeEngine().probe(
      packageInfo,
      version2Definition(
        archivePath = "classes.dex",
        captureType = LibraryInsightDefinitionValidator.CAPTURE_PREFIXED_SEMVER,
        prefix = "AndroidXMedia3/"
      )
    )

    assertEquals(listOf("1.10.1"), result.values["versions"])
  }

  @Test
  fun `hashes a complete package entry for remote lookup`() = runBlocking {
    val packageInfo = packageInfoWithEntries(
      baseEntries = mapOf(
        ANDROIDX_TEST_MODULE_PATH to "module".toByteArray()
      )
    )

    val result = LibraryInsightProbeEngine().probe(
      packageInfo,
      version2Definition(
        archivePath = ANDROIDX_TEST_MODULE_PATH,
        captureType = LibraryInsightDefinitionValidator.CAPTURE_SHA256,
        readerOperator = LibraryInsightDefinitionValidator.READER_FILE_SHA256
      )
    )

    assertEquals(listOf(MODULE_SHA256), result.values["versions"])
  }

  @Test
  fun `resolves AndroidX Test fingerprint candidates from remote rules`() = runBlocking {
    val definition = version2Definition(
      archivePath = ANDROIDX_TEST_MODULE_PATH,
      captureType = LibraryInsightDefinitionValidator.CAPTURE_SHA256,
      readerOperator = LibraryInsightDefinitionValidator.READER_FILE_SHA256,
      output = "fingerprints",
      lookups = listOf(
        LibraryInsightDefinition.Lookup(
          input = "fingerprints",
          pathTemplate = ANDROIDX_LOOKUP_PATH,
          expectedField = "sha256",
          itemsField = "fingerprints",
          maxRequests = 4,
          maxItems = 50,
          outputs = listOf(
            LibraryInsightDefinition.Output(
              output = "versions",
              field = "version"
            )
          )
        )
      )
    )
    val repository = FakeLibraryInsightRepository(
      catalog = catalog(
        definitionPath = ANDROIDX_DEFINITION_PATH,
        sdkId = ANDROIDX_SDK_ID,
        libraryUuid = ANDROIDX_LIBRARY_UUID
      ),
      definition = definition,
      definitionPath = ANDROIDX_DEFINITION_PATH,
      lookupDocuments = mapOf(
        ANDROIDX_LOOKUP_PATH to mapOf(
          "fingerprints" to listOf(
            mapOf(
              "sha256" to MODULE_SHA256,
              "version" to "1.7.0"
            ),
            mapOf(
              "sha256" to MODULE_SHA256,
              "version" to "1.7.0-rc01"
            )
          )
        )
      )
    )

    val result = ResolveLibraryInsightUseCase(
      repository = repository,
      validator = validator,
      probeEngine = LibraryInsightProbeEngine(),
      resolveLanguageAndScript = { "zh-Hans" }
    )(
      libraryUuid = ANDROIDX_LIBRARY_UUID,
      packageInfo = packageInfoWithEntries(
        baseEntries = mapOf(
          ANDROIDX_TEST_MODULE_PATH to "module".toByteArray()
        )
      ),
      localeTag = "zh-CN"
    )

    val content = (result as LibraryInsightResult.Content).content
    assertEquals("版本", content.summary.single().label)
    assertEquals(listOf("1.7.0", "1.7.0-rc01"), content.summary.single().values)
    assertEquals(listOf(ANDROIDX_LOOKUP_PATH), repository.requestedLookupPaths)
    assertFalse(repository.requestedLookupPaths.single().contains(MODULE_SHA256))
  }

  @Test
  fun `does not emit a digest for an oversized package entry`() = runBlocking {
    val packageInfo = packageInfoWithEntries(
      baseEntries = mapOf(
        ANDROIDX_TEST_MODULE_PATH to "oversized".toByteArray()
      )
    )

    val result = LibraryInsightProbeEngine().probe(
      packageInfo,
      version2Definition(
        archivePath = ANDROIDX_TEST_MODULE_PATH,
        captureType = LibraryInsightDefinitionValidator.CAPTURE_SHA256,
        readerOperator = LibraryInsightDefinitionValidator.READER_FILE_SHA256,
        maxBytesPerFile = 4
      )
    )

    assertTrue(result.evidenceFound)
    assertTrue(result.values["versions"].isNullOrEmpty())
  }

  @Test
  fun `rejects arbitrary prefixes and invalid reader capture combinations`() {
    assertFalse(
      validator.isValid(
        version2Definition(
          archivePath = ANDROIDX_VERSION_PATH,
          captureType = LibraryInsightDefinitionValidator.CAPTURE_SEMVER,
          prefix = "unexpected"
        ),
        ANDROIDX_SDK_ID,
        ANDROIDX_LIBRARY_UUID
      )
    )
    assertFalse(
      validator.isValid(
        version2Definition(
          archivePath = ANDROIDX_TEST_MODULE_PATH,
          captureType = LibraryInsightDefinitionValidator.CAPTURE_SHA256,
          readerOperator = LibraryInsightDefinitionValidator.READER_FILE_SHA256,
          output = "fingerprints",
          lookups = listOf(
            LibraryInsightDefinition.Lookup(
              input = "fingerprints",
              pathTemplate = "sdk-details/sdks/androidx_test/data/fingerprint/{value}.json",
              expectedField = "sha256",
              itemsField = "fingerprints",
              maxRequests = 1,
              maxItems = 1,
              outputs = listOf(
                LibraryInsightDefinition.Output(
                  output = "versions",
                  field = "version"
                )
              )
            )
          )
        ),
        ANDROIDX_SDK_ID,
        ANDROIDX_LIBRARY_UUID
      )
    )
    assertFalse(
      validator.isValid(
        version2Definition(
          archivePath = ANDROIDX_VERSION_PATH,
          captureType = LibraryInsightDefinitionValidator.CAPTURE_SHA256
        ),
        ANDROIDX_SDK_ID,
        ANDROIDX_LIBRARY_UUID
      )
    )
  }

  @Test
  fun `resolves lookup values and localized presentation fields`() = runBlocking {
    val repository = FakeLibraryInsightRepository(
      catalog = catalog(),
      definition = definition(),
      lookupDocuments = mapOf(
        lookupPath() to mapOf(
          "engine" to ENGINE_REVISION,
          "releases" to listOf(
            mapOf(
              "flutter" to "3.22.0",
              "dart" to "3.4.0",
              "channel" to "stable"
            )
          )
        )
      )
    )
    var supported = false

    val result = ResolveLibraryInsightUseCase(
      repository = repository,
      validator = validator,
      probeEngine = LibraryInsightProbeEngine(),
      resolveLanguageAndScript = { "zh-Hans" }
    )(
      libraryUuid = LIBRARY_UUID,
      packageInfo = packageInfoWithEngine("\u0000$ENGINE_REVISION\u00003.22.1 (stable)\u0000"),
      localeTag = "zh-CN",
      onSupported = { supported = true }
    )

    assertTrue(supported)
    assertEquals(listOf(lookupPath()), repository.requestedLookupPaths)
    val content = (result as LibraryInsightResult.Content).content
    assertEquals("Flutter", content.summary[0].label)
    assertEquals(listOf("3.22.0"), content.summary[0].values)
    assertEquals(listOf("3.22.1", "3.4.0"), content.summary[1].values)
    assertEquals("渠道", content.details[0].label)
    assertEquals(listOf("stable"), content.details[0].values)
  }

  @Test
  fun `does not treat a malformed lookup envelope as an item`() = runBlocking {
    val repository = FakeLibraryInsightRepository(
      catalog = catalog(),
      definition = definition(),
      lookupDocuments = mapOf(
        lookupPath() to mapOf(
          "engine" to ENGINE_REVISION,
          "flutter" to "3.22.0"
        )
      )
    )

    val result = ResolveLibraryInsightUseCase(
      repository = repository,
      validator = validator,
      probeEngine = LibraryInsightProbeEngine(),
      resolveLanguageAndScript = { null }
    )(
      libraryUuid = LIBRARY_UUID,
      packageInfo = packageInfoWithEngine("\u0000$ENGINE_REVISION\u0000"),
      localeTag = "en"
    )

    assertEquals(LibraryInsightResult.Unavailable, result)
  }

  private fun packageInfoWithEngine(content: String): PackageInfo {
    val base = createArchive(
      name = "base.apk",
      entries = mapOf("classes.dex" to byteArrayOf(0))
    )
    val split = createArchive(
      name = "split_config.arm64_v8a.apk",
      entries = mapOf(ENGINE_ARCHIVE_PATH to content.toByteArray())
    )
    return PackageInfo().apply {
      splitNames = arrayOf("config.arm64_v8a")
      applicationInfo = ApplicationInfo().apply {
        sourceDir = base.path
        splitSourceDirs = arrayOf(split.path)
        enabled = true
      }
    }
  }

  private fun packageInfoWithEntries(
    baseEntries: Map<String, ByteArray> = mapOf("classes.dex" to byteArrayOf(0)),
    splitEntries: Map<String, ByteArray> = emptyMap()
  ): PackageInfo {
    val base = createArchive(
      name = "base.apk",
      entries = baseEntries
    )
    val split = splitEntries.takeIf(Map<String, ByteArray>::isNotEmpty)?.let {
      createArchive(
        name = "split_config.apk",
        entries = it
      )
    }
    return PackageInfo().apply {
      if (split != null) {
        splitNames = arrayOf("config")
      }
      applicationInfo = ApplicationInfo().apply {
        sourceDir = base.path
        splitSourceDirs = split?.let { arrayOf(it.path) }
        enabled = true
      }
    }
  }

  private fun createArchive(
    name: String,
    entries: Map<String, ByteArray>
  ): File {
    val file = temporaryFolder.newFile(name)
    ZipOutputStream(file.outputStream()).use { output ->
      entries.forEach { (entryName, content) ->
        output.putNextEntry(ZipEntry(entryName))
        output.write(content)
        output.closeEntry()
      }
    }
    return file
  }

  private fun catalog(
    definitionPath: String = DEFINITION_PATH,
    sdkId: String = SDK_ID,
    libraryUuid: String = LIBRARY_UUID
  ) = LibraryInsightCatalog(
    schemaVersion = LibraryInsightDefinitionValidator.SUPPORTED_SCHEMA_VERSION,
    entries = listOf(
      LibraryInsightCatalog.Entry(
        sdkId = sdkId,
        libraryUuids = listOf(libraryUuid),
        definition = definitionPath
      )
    )
  )

  private fun definition() = LibraryInsightDefinition(
    schemaVersion = LibraryInsightDefinitionValidator.SUPPORTED_SCHEMA_VERSION,
    sdkId = SDK_ID,
    targetUuids = listOf(LIBRARY_UUID),
    probes = listOf(
      LibraryInsightDefinition.Probe(
        id = "flutter_engine",
        source = LibraryInsightDefinition.Source(
          operator = LibraryInsightDefinitionValidator.SOURCE_PACKAGE_FILE,
          fileName = "libflutter.so",
          archivePaths = listOf(ENGINE_ARCHIVE_PATH)
        ),
        reader = LibraryInsightDefinition.Reader(
          operator = LibraryInsightDefinitionValidator.READER_ASCII_STRINGS,
          maxBytesPerFile = 1024,
          maxTotalBytes = 2048
        ),
        captures = listOf(
          LibraryInsightDefinition.Capture(
            output = "engine_revisions",
            type = LibraryInsightDefinitionValidator.CAPTURE_SHA1,
            maxResults = 2
          ),
          LibraryInsightDefinition.Capture(
            output = "dart_versions",
            type = LibraryInsightDefinitionValidator.CAPTURE_SEMVER_CHANNEL,
            maxResults = 2
          )
        )
      )
    ),
    lookups = listOf(
      LibraryInsightDefinition.Lookup(
        input = "engine_revisions",
        pathTemplate = LOOKUP_PATH_TEMPLATE,
        expectedField = "engine",
        itemsField = "releases",
        maxRequests = 2,
        maxItems = 20,
        outputs = listOf(
          LibraryInsightDefinition.Output(
            output = "flutter_versions",
            field = "flutter"
          ),
          LibraryInsightDefinition.Output(
            output = "dart_versions",
            field = "dart"
          ),
          LibraryInsightDefinition.Output(
            output = "channels",
            field = "channel"
          )
        )
      )
    ),
    presentation = LibraryInsightDefinition.Presentation(
      summary = listOf(
        LibraryInsightDefinition.Field(
          label = mapOf("default" to "Flutter"),
          source = "flutter_versions",
          maxValues = 3
        ),
        LibraryInsightDefinition.Field(
          label = mapOf("default" to "Dart"),
          source = "dart_versions",
          maxValues = 3
        )
      ),
      details = listOf(
        LibraryInsightDefinition.Field(
          label = mapOf(
            "default" to "Channel",
            "zh-Hans" to "渠道"
          ),
          source = "channels",
          maxValues = 3
        )
      )
    )
  )

  private fun version2Definition(
    archivePath: String,
    captureType: String,
    readerOperator: String = LibraryInsightDefinitionValidator.READER_ASCII_STRINGS,
    prefix: String? = null,
    maxBytesPerFile: Long = 1024,
    output: String = "versions",
    lookups: List<LibraryInsightDefinition.Lookup> = emptyList()
  ) = LibraryInsightDefinition(
    schemaVersion = 2,
    sdkId = ANDROIDX_SDK_ID,
    targetUuids = listOf(ANDROIDX_LIBRARY_UUID),
    probes = listOf(
      LibraryInsightDefinition.Probe(
        id = "androidx_version",
        source = LibraryInsightDefinition.Source(
          operator = LibraryInsightDefinitionValidator.SOURCE_PACKAGE_FILE,
          fileName = File(archivePath).name,
          archivePaths = listOf(archivePath)
        ),
        reader = LibraryInsightDefinition.Reader(
          operator = readerOperator,
          maxBytesPerFile = maxBytesPerFile,
          maxTotalBytes = 2048
        ),
        captures = listOf(
          LibraryInsightDefinition.Capture(
            output = output,
            type = captureType,
            prefix = prefix,
            maxResults = 2
          )
        )
      )
    ),
    lookups = lookups,
    presentation = LibraryInsightDefinition.Presentation(
      summary = listOf(
        LibraryInsightDefinition.Field(
          label = mapOf(
            "default" to "Version",
            "zh-Hans" to "版本"
          ),
          source = "versions",
          maxValues = 2
        )
      )
    )
  )

  private fun lookupPath(): String {
    return LOOKUP_PATH_TEMPLATE.replace(
      LibraryInsightDefinitionValidator.VALUE_PLACEHOLDER,
      ENGINE_REVISION
    )
  }

  private class FakeLibraryInsightRepository(
    private val catalog: LibraryInsightCatalog,
    private val definition: LibraryInsightDefinition,
    private val definitionPath: String = DEFINITION_PATH,
    private val lookupDocuments: Map<String, Map<String, Any?>>
  ) : LibraryInsightRepository {

    val requestedLookupPaths = mutableListOf<String>()

    override suspend fun getCatalog(): RemoteDocumentResult<LibraryInsightCatalog> {
      return RemoteDocumentResult.Success(catalog)
    }

    override suspend fun getDefinition(path: String): RemoteDocumentResult<LibraryInsightDefinition> {
      return if (path == definitionPath) {
        RemoteDocumentResult.Success(definition)
      } else {
        RemoteDocumentResult.NotFound
      }
    }

    override suspend fun getLookup(path: String): RemoteDocumentResult<Map<String, Any?>> {
      requestedLookupPaths += path
      return lookupDocuments[path]?.let { RemoteDocumentResult.Success(it) }
        ?: RemoteDocumentResult.NotFound
    }
  }

  private companion object {
    const val SDK_ID = "flutter"
    const val LIBRARY_UUID = "AEF9680F-4A43-4EDC-A5B8-8119D23BCD21"
    const val ENGINE_REVISION = "d3ea636dc5d16b56819f3266241e1f708979c233"
    const val ENGINE_ARCHIVE_PATH = "lib/arm64-v8a/libflutter.so"
    const val DEFINITION_PATH = "sdk-details/sdks/flutter/definition.json"
    const val LOOKUP_PATH_TEMPLATE = "sdk-details/sdks/flutter/data/engine/{value}.json"
    const val ANDROIDX_SDK_ID = "androidx_activity"
    const val ANDROIDX_LIBRARY_UUID = "DF93DF56-63D0-4D3B-AF4B-39CA3C785A18"
    const val ANDROIDX_VERSION_PATH = "META-INF/androidx.activity_activity.version"
    const val ANDROIDX_TEST_MODULE_PATH = "META-INF/androidx.test.core.kotlin_module"
    const val MODULE_SHA256 = "120970d812836f19888625587a4606a5ad23cef31c8684e601771552548fc6b9"
    const val ANDROIDX_DEFINITION_PATH = "sdk-details/sdks/androidx_test/definition.json"
    const val ANDROIDX_LOOKUP_PATH =
      "sdk-details/sdks/androidx_test/data/fingerprints.json"
  }
}
