package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import com.absinthe.libchecker.domain.app.detail.resource.AppResourceReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPropItemTest {

  @Test
  fun originalStateExposesPreparedDisplayValue() {
    val item = AppPropItem(
      key = "android.adservices.AD_SERVICES_CONFIG",
      originalDisplayValue = "@xml/ad_services_config",
      resource = AppResourceReference.create(0x7f120001, "xml")
    )

    assertEquals("@xml/ad_services_config", item.visibleValue)
    assertEquals(
      "android.adservices.AD_SERVICES_CONFIG, @xml/ad_services_config",
      item.contentDescription
    )
    assertFalse(item.isTransformed)
  }

  @Test
  fun textPreviewAndRestoreProduceCompleteRowStates() {
    val original = AppPropItem(
      key = "feature_enabled",
      originalDisplayValue = "@bool/feature_enabled",
      resource = AppResourceReference.create(0x7f050001, "bool")
    )
    val preview = original.copy(preview = AppResourcePreview.Text("true"))

    assertEquals("true", preview.visibleValue)
    assertEquals("feature_enabled, true", preview.contentDescription)
    assertTrue(preview.isTransformed)
    assertEquals(original, preview.restore())
  }

  @Test
  fun colorPreviewKeepsVisibleResourceNameUntilRestored() {
    val preview = AppPropItem(
      key = "accent_color",
      originalDisplayValue = "@color/accent",
      resource = AppResourceReference.create(0x7f060001, "color"),
      preview = AppResourcePreview.ColorValue(0xff336699.toInt())
    )

    assertEquals("@color/accent", preview.visibleValue)
    assertEquals("accent_color, @color/accent", preview.contentDescription)
    assertTrue(preview.isTransformed)
  }

  @Test
  fun resourceReferenceOnlyAcceptsPreviewableTypes() {
    assertNotNull(AppResourceReference.create(1, "drawable"))
    assertNull(AppResourceReference.create(1, "layout"))
    assertNull(AppResourceReference.create(null, "string"))
    assertNull(AppResourceReference.create(1, null))
  }
}
