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
    definitionPath: String = DEFINITION_PATH
  ) = LibraryInsightCatalog(
    schemaVersion = LibraryInsightDefinitionValidator.SUPPORTED_SCHEMA_VERSION,
    entries = listOf(
      LibraryInsightCatalog.Entry(
        sdkId = SDK_ID,
        libraryUuids = listOf(LIBRARY_UUID),
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

  private fun lookupPath(): String {
    return LOOKUP_PATH_TEMPLATE.replace(
      LibraryInsightDefinitionValidator.VALUE_PLACEHOLDER,
      ENGINE_REVISION
    )
  }

  private class FakeLibraryInsightRepository(
    private val catalog: LibraryInsightCatalog,
    private val definition: LibraryInsightDefinition,
    private val lookupDocuments: Map<String, Map<String, Any?>>
  ) : LibraryInsightRepository {

    val requestedLookupPaths = mutableListOf<String>()

    override suspend fun getCatalog(): RemoteDocumentResult<LibraryInsightCatalog> {
      return RemoteDocumentResult.Success(catalog)
    }

    override suspend fun getDefinition(path: String): RemoteDocumentResult<LibraryInsightDefinition> {
      return if (path == DEFINITION_PATH) {
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
  }
}
