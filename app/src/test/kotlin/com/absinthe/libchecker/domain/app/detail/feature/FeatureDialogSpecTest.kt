package com.absinthe.libchecker.domain.app.detail.feature

import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureDialogSpecTest {

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
}
