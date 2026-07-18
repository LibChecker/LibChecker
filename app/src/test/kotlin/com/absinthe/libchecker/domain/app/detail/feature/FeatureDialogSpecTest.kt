package com.absinthe.libchecker.domain.app.detail.feature

import com.absinthe.libchecker.R
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureDialogSpecTest {

  @Test
  fun reactiveDialogListsEveryDetectedLibraryWithoutBrandTints() {
    val spec = AppDetailFeatureAction.Reactive(
      listOf(
        ReactiveLibrary(ReactiveType.RX_JAVA, "3"),
        ReactiveLibrary(ReactiveType.RX_ANDROID, null)
      )
    ).toDialogSpec()

    assertEquals(R.drawable.ic_reactivex, spec.iconRes)
    assertEquals(null, spec.iconTint)
    assertEquals(R.string.reactivex, spec.titleRes)
    assertEquals(null, spec.messageRes)
    assertEquals(FeatureDialogEntryPlacement.MESSAGE, spec.entryPlacement)
    assertEquals(
      listOf(
        FeatureDialogTitleEntry(
          label = FeatureDialogTitleLabel.Resource(R.string.rxjava),
          value = "3"
        ),
        FeatureDialogTitleEntry(
          label = FeatureDialogTitleLabel.Resource(R.string.rxandroid),
          value = null
        )
      ),
      spec.titleEntries
    )
    assertEquals("https://reactivex.io/", spec.sourceUrl)
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
}
