package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetsSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticManifestAttributeQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticManifestElement
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticRemoteManifest
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringPattern
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
  fun `installs and reloads a recursive artifact condition`() {
    val definition = officialDefinition().copy(
      id = "official.artifact-evidence",
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.PREDICATE,
        predicate = StatisticPredicateSpec(
          condition = StatisticConditionSpec(
            any = listOf(
              StatisticConditionSpec(
                evidence = StatisticEvidence.DEX_CLASS,
                operator = StatisticComparisonOperator.CONTAINS_ANY,
                value = StatisticPredicateValue(
                  dexClasses = listOf(
                    StatisticDexClassQuery(
                      name = StatisticStringPattern(
                        operator = StatisticStringOperator.STARTS_WITH,
                        value = "Lcom/example/"
                      )
                    )
                  )
                )
              ),
              StatisticConditionSpec(
                evidence = StatisticEvidence.MANIFEST_RECEIVER_ACTION,
                operator = StatisticComparisonOperator.CONTAINS_ANY,
                value = StatisticPredicateValue(strings = listOf("com.example.ACTION"))
              )
            )
          ),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Matched")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Other"))
        )
      )
    )
    val store = createStore()
    val bundleFile = createBundle(VALID_SVG, definition = definition)

    val installed = store.install(manifestFor(bundleFile), bundleFile)
    val cached = store.loadCachedStatistics()

    assertEquals(listOf("official.artifact-evidence"), installed.map { it.id })
    assertNotNull(cached.single().calculation.predicate?.condition?.any)
  }

  @Test
  fun `installs and reloads an ordered facets calculation`() {
    val definition = officialDefinition().copy(
      id = "official.capabilities",
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.FACETS,
        facets = StatisticFacetsSpec(
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Matched")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Other")),
          items = listOf(
            StatisticFacetSpec(
              id = "voip-service-kit",
              title = StatisticTitleSpec(translations = mapOf("en" to "VoIP Service Kit")),
              condition = StatisticConditionSpec(
                evidence = StatisticEvidence.DEX_CLASS,
                operator = StatisticComparisonOperator.CONTAINS_ANY,
                value = StatisticPredicateValue(
                  dexClasses = listOf(
                    StatisticDexClassQuery(
                      name = StatisticStringPattern(
                        operator = StatisticStringOperator.STARTS_WITH,
                        value = "Lcom/voip/service/"
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
    val store = createStore()
    val bundleFile = createBundle(VALID_SVG, definition = definition)

    val installed = store.install(manifestFor(bundleFile), bundleFile)
    val cached = store.loadCachedStatistics()

    assertEquals(listOf("official.capabilities"), installed.map { it.id })
    assertEquals("voip-service-kit", cached.single().calculation.facets?.items?.single()?.id)
  }

  @Test
  fun `installs and reloads a manifest attribute condition`() {
    val attribute = StatisticManifestAttributeQuery(
      element = StatisticManifestElement.APPLICATION,
      name = "android:enableOnBackInvokedCallback",
      boolean = true
    )
    val definition = officialDefinition().copy(
      id = "official.predictive-back-gesture",
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.PREDICATE,
        predicate = StatisticPredicateSpec(
          evidence = StatisticEvidence.MANIFEST_ATTRIBUTE,
          operator = StatisticComparisonOperator.EQUAL,
          value = StatisticPredicateValue(manifestAttribute = attribute),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Enabled")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Not enabled"))
        )
      )
    )
    val store = createStore()
    val bundleFile = createBundle(VALID_SVG, definition = definition)

    val installed = store.install(manifestFor(bundleFile), bundleFile)
    val cached = store.loadCachedStatistics()

    assertEquals(listOf("official.predictive-back-gesture"), installed.map { it.id })
    assertEquals(attribute, cached.single().calculation.predicate?.value?.manifestAttribute)
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
    includeSecondIcon: Boolean = false,
    definition: StatisticDefinition = officialDefinition()
  ): File {
    val file = temporaryFolder.newFile(name)
    val definitions = buildList {
      add(definition)
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
