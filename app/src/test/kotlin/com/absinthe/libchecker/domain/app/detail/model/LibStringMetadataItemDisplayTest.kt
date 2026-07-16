package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibStringMetadataItemDisplayTest {

  @Test
  fun createsLinkableInstalledResourceState() {
    val display = LibStringMetadataItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(
          name = "android.adservices.AD_SERVICES_CONFIG",
          size = 0x7f120001,
          source = "com.example:xml/ad_services_config"
        ),
        rule = null,
        labels = listOf("xml")
      ),
      itemDisplayOptions = AdvancedOptions.ITEM_DEFAULT_OPTIONS,
      apkPreviewUnavailableLabel = "Not available"
    )

    assertEquals("com.example:xml/ad_services_config", display.visibleValue)
    assertNotNull(display.resource)
    assertEquals("xml", display.resource?.type)
    assertFalse(display.isTransformed)
  }

  @Test
  fun textPreviewAndRestoreAreImmutableRowStates() {
    val original = LibStringMetadataItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(name = "feature_enabled", size = 0x7f050001, source = "@bool/feature_enabled"),
        rule = null,
        labels = listOf("bool")
      ),
      itemDisplayOptions = 0,
      apkPreviewUnavailableLabel = "Not available"
    )
    val preview = original.copy(preview = AppResourcePreview.Text("true"))

    assertEquals("true", preview.visibleValue)
    assertEquals("feature_enabled, true", preview.contentDescription)
    assertTrue(preview.isTransformed)
    assertEquals(original, preview.restore())
  }

  @Test
  fun apkPreviewStateCannotExposeResourceAction() {
    val display = LibStringMetadataItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(name = "resource_key", size = -1L),
        rule = null,
        labels = listOf("xml")
      ),
      itemDisplayOptions = 0,
      apkPreviewUnavailableLabel = "Not available"
    )

    assertEquals("<Not available>", display.visibleValue)
    assertNull(display.resource)
  }
}
