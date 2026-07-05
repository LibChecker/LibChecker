package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.options.AdvancedOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class LibStringItemNameDisplayTest {

  @Test
  fun keepsPlainNameWithoutMarkerSource() {
    assertEquals(
      LibStringItemNameDisplay(
        text = "ExampleService",
        decoration = LibStringItemNameDisplay.Decoration.Plain
      ),
      LibStringItemNameDisplay.create(
        item = LibStringItem(name = "ExampleService"),
        type = SERVICE,
        itemDisplayOptions = AdvancedOptions.MARK_DISABLED or AdvancedOptions.MARK_EXPORTED
      )
    )
  }

  @Test
  fun marksDisabledItemWhenDisabledOptionIsEnabled() {
    assertEquals(
      LibStringItemNameDisplay.Decoration.Disabled,
      LibStringItemNameDisplay.create(
        item = LibStringItem(name = "DisabledService", source = DISABLED),
        type = SERVICE,
        itemDisplayOptions = AdvancedOptions.MARK_DISABLED
      ).decoration
    )
  }

  @Test
  fun keepsDisabledComponentPlainWhenDisabledOptionIsOff() {
    assertEquals(
      LibStringItemNameDisplay.Decoration.Plain,
      LibStringItemNameDisplay.create(
        item = LibStringItem(name = "DisabledService", source = DISABLED),
        type = SERVICE,
        itemDisplayOptions = 0
      ).decoration
    )
  }

  @Test
  fun alwaysMarksDisabledPermissionItems() {
    assertEquals(
      LibStringItemNameDisplay.Decoration.Disabled,
      LibStringItemNameDisplay.create(
        item = LibStringItem(name = "android.permission.CAMERA", source = DISABLED),
        type = PERMISSION,
        itemDisplayOptions = 0
      ).decoration
    )
  }

  @Test
  fun marksExportedItemWhenExportedOptionIsEnabled() {
    assertEquals(
      LibStringItemNameDisplay.Decoration.Exported,
      LibStringItemNameDisplay.create(
        item = LibStringItem(name = "ExportedService", source = EXPORTED),
        type = SERVICE,
        itemDisplayOptions = AdvancedOptions.MARK_EXPORTED
      ).decoration
    )
  }

  @Test
  fun keepsExportedItemPlainWhenExportedOptionIsOff() {
    assertEquals(
      LibStringItemNameDisplay.Decoration.Plain,
      LibStringItemNameDisplay.create(
        item = LibStringItem(name = "ExportedService", source = EXPORTED),
        type = SERVICE,
        itemDisplayOptions = 0
      ).decoration
    )
  }
}
