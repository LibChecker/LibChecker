package com.absinthe.libchecker.domain.app.detail.model

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
      resource = AppPropResourceReference.create(0x7f120001, "xml")
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
      resource = AppPropResourceReference.create(0x7f050001, "bool")
    )
    val preview = original.copy(preview = AppPropPreview.Text("true"))

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
      resource = AppPropResourceReference.create(0x7f060001, "color"),
      preview = AppPropPreview.ColorValue(0xff336699.toInt())
    )

    assertEquals("@color/accent", preview.visibleValue)
    assertEquals("accent_color, @color/accent", preview.contentDescription)
    assertTrue(preview.isTransformed)
  }

  @Test
  fun resourceReferenceOnlyAcceptsPreviewableTypes() {
    assertNotNull(AppPropResourceReference.create(1, "drawable"))
    assertNull(AppPropResourceReference.create(1, "layout"))
    assertNull(AppPropResourceReference.create(null, "string"))
    assertNull(AppPropResourceReference.create(1, null))
  }
}
