package com.absinthe.libchecker.domain.app.detail.feature

import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinBuildMetadata
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinVersionSource
import com.absinthe.libchecker.domain.app.model.AppFunctionInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureDialogSpecTest {

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
  fun kotlinDialogPreservesAllPluginTitleEntriesInOrder() {
    val extras = linkedMapOf(
      "Kotlin" to "2.2.0",
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
