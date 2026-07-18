package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticRemoteManifest
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticCatalogUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticSvgUseCase
import com.absinthe.libchecker.utils.JsonUtil
import com.absinthe.libchecker.utils.extensions.sha256
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfficialStatisticBundleStoreTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `installs and reloads a verified official bundle`() {
    val store = createStore()
    val bundleFile = createBundle(VALID_SVG)
    val manifest = manifestFor(bundleFile)

    val installed = store.install(manifest, bundleFile)
    val cached = store.loadCachedStatistics()

    assertEquals(listOf("official.target-sdk-35-plus"), installed.map { it.id })
    assertEquals(installed, cached)
    assertEquals(manifest.bundleSha256, store.currentSha256)
    assertNotNull(cached.single().icon.localPath)
    assertEquals(true, File(checkNotNull(cached.single().icon.localPath)).isFile)
  }

  @Test
  fun `installs multiple icons sharing one directory`() {
    val store = createStore()
    val bundleFile = createBundle(VALID_SVG, includeSecondIcon = true)

    val installed = store.install(manifestFor(bundleFile), bundleFile)

    assertEquals(
      listOf("official.target-sdk-35-plus", "official.target-sdk-36-plus"),
      installed.map { it.id }
    )
    assertTrue(installed.all { File(checkNotNull(it.icon.localPath)).isFile })
  }

  @Test
  fun `rejects a digest mismatch`() {
    val store = createStore()
    val bundleFile = createBundle(VALID_SVG)
    val manifest = manifestFor(bundleFile).copy(bundleSha256 = "0".repeat(64))

    assertThrows(IllegalStateException::class.java) {
      store.install(manifest, bundleFile)
    }
    assertEquals(null, store.currentSha256)
  }

  @Test
  fun `invalid SVG does not replace the current bundle`() {
    val store = createStore()
    val validBundle = createBundle(VALID_SVG, "valid.bundle")
    val validManifest = manifestFor(validBundle)
    store.install(validManifest, validBundle)
    val invalidBundle = createBundle(INVALID_SVG, "invalid.bundle")

    assertThrows(IllegalStateException::class.java) {
      store.install(manifestFor(invalidBundle, version = 2), invalidBundle)
    }

    assertEquals(validManifest.bundleSha256, store.currentSha256)
    assertEquals(listOf("official.target-sdk-35-plus"), store.loadCachedStatistics().map { it.id })
  }

  private fun createStore() = OfficialStatisticBundleStore(
    rootDirectory = temporaryFolder.newFolder("store-${System.nanoTime()}"),
    validateCatalog = ValidateStatisticCatalogUseCase(),
    validateSvg = ValidateStatisticSvgUseCase()
  )

  private fun createBundle(
    svg: String,
    name: String = "chart.bundle",
    includeSecondIcon: Boolean = false
  ): File {
    val file = temporaryFolder.newFile(name)
    val definitions = buildList {
      add(officialDefinition())
      if (includeSecondIcon) {
        add(
          officialDefinition().copy(
            id = "official.target-sdk-36-plus",
            icon = StatisticIconSpec(asset = "icons/android-16.svg")
          )
        )
      }
    }
    val catalog = JsonUtil.toJson(StatisticBundle(1, definitions))
      .orEmpty()
      .encodeToByteArray()
    ZipOutputStream(file.outputStream()).use { output ->
      output.putNextEntry(ZipEntry("catalog.json"))
      output.write(catalog)
      output.closeEntry()
      output.putNextEntry(ZipEntry("icons/android-15.svg"))
      output.write(svg.encodeToByteArray())
      output.closeEntry()
      if (includeSecondIcon) {
        output.putNextEntry(ZipEntry("icons/android-16.svg"))
        output.write(svg.encodeToByteArray())
        output.closeEntry()
      }
    }
    return file
  }

  private fun manifestFor(
    bundle: File,
    version: Int = 1
  ) = StatisticRemoteManifest(
    schemaVersion = 1,
    bundleVersion = version,
    bundleSha256 = bundle.readBytes().sha256().lowercase(),
    bundleSize = bundle.length(),
    minimumAppVersionCode = 0
  )

  private fun officialDefinition() = StatisticDefinition(
    id = "official.target-sdk-35-plus",
    revision = 1,
    source = StatisticSource.OFFICIAL,
    title = StatisticTitleSpec(translations = mapOf("en" to "Target SDK 35+")),
    icon = StatisticIconSpec(asset = "icons/android-15.svg"),
    calculation = StatisticCalculationSpec(
      kind = StatisticCalculationKind.PREDICATE,
      predicate = StatisticPredicateSpec(
        evidence = StatisticEvidence.TARGET_SDK,
        operator = StatisticComparisonOperator.GREATER_THAN_OR_EQUAL,
        value = StatisticPredicateValue(integer = 35),
        matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Modern")),
        unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Legacy"))
      )
    )
  )

  private companion object {
    const val VALID_SVG = """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
        <path fill="#000000" d="M4 20V10h4v10H4Z"/>
      </svg>
    """
    const val INVALID_SVG = """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
        <script>run()</script>
      </svg>
    """
  }
}
