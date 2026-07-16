package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureItemTest {

  @Test
  fun itemCarriesExplicitIconAndActionModels() {
    val item = FeatureItem(
      titleRes = R.string.app_bundle,
      icon = FeatureItemIcon.Resource(R.drawable.ic_aab),
      action = AppDetailFeatureAction.SplitApks
    )

    assertEquals(FeatureItemIcon.Resource(R.drawable.ic_aab), item.icon)
    assertEquals(AppDetailFeatureAction.SplitApks, item.action)
  }
}
