package com.absinthe.libchecker.domain.app.detail.feature

import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

  @Test
  fun reactiveLibrariesUseOneUntintedFeatureItem() {
    val item = BuildAppDetailFeatureItemUseCase()(
      request(
        VersionedFeature(
          featureType = Features.Ext.REACTIVE,
          extras = linkedMapOf(
            ReactiveType.RX_JAVA.key to "3",
            ReactiveType.RX_KOTLIN.key to "3.0.1",
            ReactiveType.RX_ANDROID.key to null
          )
        )
      )
    )

    assertEquals(AppDetailFeatureIcon.Resource(R.drawable.ic_reactivex), item?.icon)
    assertEquals(R.string.reactivex, item?.titleRes)
    assertEquals(
      AppDetailFeatureAction.Reactive(
        listOf(
          ReactiveLibrary(ReactiveType.RX_JAVA, "3"),
          ReactiveLibrary(ReactiveType.RX_KOTLIN, "3.0.1"),
          ReactiveLibrary(ReactiveType.RX_ANDROID, null)
        )
      ),
      item?.action
    )
  }

  @Test
  fun individualReactiveFlagsNoLongerCreateFeatureItems() {
    assertNull(BuildAppDetailFeatureItemUseCase()(request(VersionedFeature(Features.RX_JAVA, "3"))))
    assertNull(BuildAppDetailFeatureItemUseCase()(request(VersionedFeature(Features.RX_KOTLIN, "3"))))
    assertNull(BuildAppDetailFeatureItemUseCase()(request(VersionedFeature(Features.RX_ANDROID, "3"))))
  }

  private fun request(feature: VersionedFeature) = AppDetailFeatureItemRequest(
    feature = feature,
    currentFeatureCount = 0,
    apkAnalyticsMode = false,
    canShowInstallSource = true,
    canShowAppIcons = true,
    appIcons = emptyList()
  )
}
