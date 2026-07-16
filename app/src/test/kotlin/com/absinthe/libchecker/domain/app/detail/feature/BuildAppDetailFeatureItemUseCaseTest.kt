package com.absinthe.libchecker.domain.app.detail.feature

import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildAppDetailFeatureItemUseCaseTest {

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
