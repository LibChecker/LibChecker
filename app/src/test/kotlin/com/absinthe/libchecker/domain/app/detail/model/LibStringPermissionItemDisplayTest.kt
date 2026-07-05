package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.constant.options.AdvancedOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibStringPermissionItemDisplayTest {

  @Test
  fun marksDeniedPermissionWithNotGrantedLabel() {
    val display = LibStringPermissionItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(
          name = "android.permission.POST_NOTIFICATIONS",
          size = 0L
        ),
        rule = null
      ),
      itemDisplayOptions = AdvancedOptions.ITEM_DEFAULT_OPTIONS,
      notGrantedLabel = "Not granted"
    )

    assertEquals(
      LibStringItemNameDisplay(
        text = "android.permission.POST_NOTIFICATIONS",
        decoration = LibStringItemNameDisplay.Decoration.Plain
      ),
      display.name
    )
    assertTrue(display.showNotGrantedIndicator)
    assertEquals("Not granted", display.notGrantedLabel)
    assertEquals(
      "android.permission.POST_NOTIFICATIONS, Not granted",
      display.contentDescription
    )
  }

  @Test
  fun omitsNotGrantedLabelForGrantedPermission() {
    val display = LibStringPermissionItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(
          name = "android.permission.INTERNET",
          size = 1L
        ),
        rule = null
      ),
      itemDisplayOptions = AdvancedOptions.ITEM_DEFAULT_OPTIONS,
      notGrantedLabel = "Not granted"
    )

    assertFalse(display.showNotGrantedIndicator)
    assertNull(display.notGrantedLabel)
    assertEquals("android.permission.INTERNET", display.contentDescription)
  }

  @Test
  fun keepsPermissionDisabledDecorationIndependentOfOption() {
    val display = LibStringPermissionItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(
          name = "android.permission.READ_PHONE_STATE",
          source = DISABLED
        ),
        rule = null
      ),
      itemDisplayOptions = 0,
      notGrantedLabel = "Not granted"
    )

    assertEquals(LibStringItemNameDisplay.Decoration.Disabled, display.name.decoration)
  }
}
