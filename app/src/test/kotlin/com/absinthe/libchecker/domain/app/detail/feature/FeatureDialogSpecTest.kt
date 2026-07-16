package com.absinthe.libchecker.domain.app.detail.feature

import com.absinthe.libchecker.R
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureDialogSpecTest {

  @Test
  fun rxKotlinDialogKeepsTintVersionAndSourceLinkAsDisplayData() {
    val spec = AppDetailFeatureAction.RxKotlin("3.0.1").toDialogSpec()

    assertEquals(R.drawable.ic_reactivex, spec.iconRes)
    assertEquals(0xFF7F52FF.toInt(), spec.iconTint)
    assertEquals(
      listOf(
        FeatureDialogTitleEntry(
          label = FeatureDialogTitleLabel.Resource(R.string.rxkotlin),
          value = "3.0.1"
        )
      ),
      spec.titleEntries
    )
    assertEquals("https://github.com/ReactiveX/RxKotlin", spec.sourceUrl)
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
