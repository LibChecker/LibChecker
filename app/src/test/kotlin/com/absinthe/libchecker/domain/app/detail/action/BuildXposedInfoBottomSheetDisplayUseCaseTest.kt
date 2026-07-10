package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.domain.app.detail.model.XposedInfoAction
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildXposedInfoBottomSheetDisplayUseCaseTest {

  @Test
  fun `builds every available module detail in display order`() {
    val display = buildXposedInfoBottomSheetDisplay(
      info = XposedModuleInfo(
        appName = "Module",
        settingsIntent = null,
        minVersion = "93",
        targetVersion = "100",
        staticScope = true,
        defaultScope = "android\ncom.example",
        javaInitClasses = "JavaEntry",
        nativeInitLibraries = "native_entry",
        legacyInitClass = "LegacyEntry",
        description = "Description"
      ),
      strings = strings
    )

    assertEquals("Module", display.appName)
    assertNull((display.settingsAction as XposedInfoAction.OpenSettings).intent)
    assertEquals(
      listOf(
        "Minimum",
        "Target",
        "Static",
        "Default",
        "Init (Java)",
        "Init (Native)",
        "Init (Legacy)",
        "Description label"
      ),
      display.items.map { it.tip }
    )
    assertEquals(
      listOf(
        "93",
        "100",
        "True",
        "android\ncom.example",
        "JavaEntry",
        "native_entry",
        "LegacyEntry",
        "Description"
      ),
      display.items.map { it.text }
    )
    assertEquals(XposedInfoTextStyle.Body, display.items.last().textStyle)
    assertEquals(
      List(7) { XposedInfoTextStyle.Title },
      display.items.dropLast(1).map { it.textStyle }
    )
  }

  @Test
  fun `omits blank optional module details`() {
    val display = buildXposedInfoBottomSheetDisplay(
      info = XposedModuleInfo(
        appName = "Module",
        settingsIntent = null,
        minVersion = null,
        targetVersion = " ",
        staticScope = false,
        defaultScope = "",
        javaInitClasses = null,
        nativeInitLibraries = null,
        legacyInitClass = null,
        description = null
      ),
      strings = strings
    )

    assertEquals(emptyList<Any>(), display.items)
  }

  private companion object {
    val strings = XposedInfoDisplayStrings(
      minVersion = "Minimum",
      targetVersion = "Target",
      staticScope = "Static",
      defaultScope = "Default",
      initClass = "Init",
      description = "Description label",
      trueValue = "True"
    )
  }
}
