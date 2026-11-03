package com.absinthe.libchecker.domain.app.detail.feature

import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildAppDetailFeatureItemUseCaseTest {

  @Test
  fun r8OnlyFeatureHasR8LabelAndDialogEntry() {
    val item = BuildAppDetailFeatureItemUseCase()(
      AppDetailFeatureItemRequest(
        feature = VersionedFeature(Features.AGP, extras = mapOf("R8" to "9.5.28 (Full)")),
        currentFeatureCount = 0,
        apkAnalyticsMode = false,
        canShowInstallSource = true,
        canShowAppIcons = true,
        appIcons = emptyList()
      )
    )!!
    assertEquals(R.string.r8, item.titleRes)
    assertEquals(
      "9.5.28 (Full)",
      (item.action as AppDetailFeatureAction.Dialog).toDialogSpec().titleEntries?.single()?.value
    )
  }

  @Test
  fun composeFeaturesPreserveTheirOwnVersionThroughDialogMapping() {
    val featureTitles = mapOf(
      Features.JETPACK_COMPOSE to R.string.jetpack_compose,
      Features.COMPOSE_MULTIPLATFORM_RESOURCES to R.string.compose_multiplatform_resources
    )
    for ((featureType, titleRes) in featureTitles) {
      for (version in listOf("1.8.2", null)) {
        val item = BuildAppDetailFeatureItemUseCase()(
          AppDetailFeatureItemRequest(
            feature = VersionedFeature(featureType, version),
            currentFeatureCount = 0,
            apkAnalyticsMode = false,
            canShowInstallSource = true,
            canShowAppIcons = true,
            appIcons = emptyList()
          )
        )
        val spec = (item!!.action as AppDetailFeatureAction.Dialog).toDialogSpec()

        assertEquals(titleRes, spec.titleRes)
        assertEquals(
          listOf(FeatureDialogTitleEntry(FeatureDialogTitleLabel.Resource(titleRes), version)),
          spec.titleEntries
        )
      }
    }
  }

  @Test
  fun resourceFeatureUsesExplicitIconModel() {
    val item = BuildAppDetailFeatureItemUseCase()(
      AppDetailFeatureItemRequest(
        feature = VersionedFeature(Features.SPLIT_APKS),
        currentFeatureCount = 0,
        apkAnalyticsMode = false,
        canShowInstallSource = true,
        canShowAppIcons = true,
        appIcons = emptyList()
      )
    )

    assertEquals(
      AppDetailFeatureIcon.Resource(R.drawable.ic_aab),
      item?.icon
    )
  }
}
