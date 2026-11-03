package com.absinthe.libchecker.domain.app.detail.feature

import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinBuildMetadata
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinVersionSource
import com.absinthe.libchecker.domain.app.model.AppFunctionInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureDialogSpecTest {

  @Test
  fun agpDialogShowsR8AsSubtitleAndSupportsR8WithoutAgp() {
    val extras = mapOf("R8" to "9.5.28 (Full)")
    assertEquals(
      listOf(FeatureDialogTitleEntry(FeatureDialogTitleLabel.Resource(R.string.agp), "9.5.0")),
      AppDetailFeatureAction.Agp("9.5.0", extras).toDialogSpec().titleEntries
    )
    val r8Only = AppDetailFeatureAction.Agp(null, extras).toDialogSpec()
    assertEquals(
      listOf(FeatureDialogTitleEntry(FeatureDialogTitleLabel.Resource(R.string.r8), "9.5.28 (Full)")),
      r8Only.titleEntries
    )
    assertEquals(null, r8Only.subtitleEntries)
    assertEquals(
      listOf(FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text("R8"), "9.5.28 (Full)")),
      AppDetailFeatureAction.Agp("9.5.0", extras).toDialogSpec().subtitleEntries
    )
    assertEquals(R.string.r8, r8Only.titleRes)
    assertEquals(R.string.r8_details, r8Only.messageRes)
    assertEquals(
      listOf(FeatureDialogTitleEntry(FeatureDialogTitleLabel.Resource(R.string.agp), "9.5.0")),
      AppDetailFeatureAction.Agp("9.5.0").toDialogSpec().titleEntries
    )
  }

  @Test
  fun r8OnlyDialogKeepsHighestVersionInTitleAndRemainingVersionsInSubtitle() {
    val spec = AppDetailFeatureAction.Agp(
      null,
      mapOf("R8" to "9.6.0-alpha02 (Full) / 9.4.5-dev (Full) / 9.1.20-dev (Full)")
    ).toDialogSpec()
    assertEquals(
      listOf(FeatureDialogTitleEntry(FeatureDialogTitleLabel.Resource(R.string.r8), "9.6.0-alpha02 (Full)")),
      spec.titleEntries
    )
    assertEquals(
      listOf(FeatureDialogTitleEntry(FeatureDialogTitleLabel.Resource(R.string.r8), "9.4.5-dev (Full) / 9.1.20-dev (Full)")),
      spec.subtitleEntries
    )
    assertEquals(R.string.r8_details, spec.messageRes)
  }

  @Test
  fun appFunctionsAreListItemsRatherThanDialogTitleMarkup() {
    val functions = listOf(AppFunctionInfo("sample.Service#search", description = "Find <items>"), AppFunctionInfo("sample.<literal>"))
    val spec = AppDetailFeatureAction.AppFunction(functions).toDialogSpec()
    assertEquals(
      listOf(
        FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text("search"), "Find <items>"),
        FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text("sample.<literal>"), null)
      ),
      spec.items
    )
    assertEquals(null, spec.titleEntries)
    assertEquals(R.string.app_function_details, spec.messageRes)
    assertEquals(R.drawable.ic_app_function, spec.itemIconRes)
  }

  @Test
  fun kotlinDialogMovesBuildVersionsToSubtitleAndPreservesPluginTitles() {
    val extras = linkedMapOf(
      "Kotlin" to "2.2.0",
      "Gradle" to "9.1.0",
      "Java" to "25",
      "Compose Compiler" to null
    )

    val spec = AppDetailFeatureAction.Kotlin(extras).toDialogSpec()

    assertEquals(
      listOf(
        FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text("Kotlin"), "2.2.0"),
        FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text("Compose Compiler"), null)
      ),
      spec.titleEntries
    )
    assertEquals(
      listOf(
        FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text("Gradle"), "9.1.0"),
        FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text("Java"), "25")
      ),
      spec.subtitleEntries
    )
  }

  @Test
  fun kotlinDialogKeepsDefaultTitleWhenOnlyBuildVersionsAreAvailable() {
    val spec = AppDetailFeatureAction.Kotlin(mapOf("Java" to "25")).toDialogSpec()
    assertEquals(null, spec.titleEntries)
    assertEquals(R.string.kotlin_string, spec.titleRes)
    assertEquals(
      listOf(FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text("Java"), "25")),
      spec.subtitleEntries
    )
  }

  @Test
  fun kotlinDialogShowsVersionWithoutInferenceSource() {
    val metadata = KotlinBuildMetadata(
      kotlinVersion = "2.1.x",
      kotlinVersionSource = KotlinVersionSource.DEX_ENTRY_POINTS
    )

    assertEquals(
      linkedMapOf("Kotlin" to "2.1.x"),
      metadata.toKotlinDialogEntries()
    )
  }
}
