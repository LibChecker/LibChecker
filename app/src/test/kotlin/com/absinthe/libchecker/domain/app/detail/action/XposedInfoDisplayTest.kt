package com.absinthe.libchecker.domain.app.detail.action

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoAction
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XposedInfoDisplayTest {

  @Test
  fun `builds every available module detail in display order`() {
    val display = buildXposedInfoBottomSheetDisplay(
      info = XposedModuleInfo(
        appName = "Module",
        settingsIntent = null,
        minVersion = "93",
        targetVersion = "102",
        autoHotReloadDeclared = true,
        staticScope = true,
        defaultScope = listOf(
          XposedScopeAppInfo(packageName = "android", label = "Android System", packageInfo = null),
          XposedScopeAppInfo(packageName = "com.example", label = "Example", packageInfo = PackageInfo()),
          XposedScopeAppInfo(packageName = "com.missing", label = "Missing", packageInfo = null)
        ),
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
        "Automatic hot reload",
        "Static",
        "Default",
        "Init (Java)",
        "Init (Native)",
        "Init (Legacy)",
        "Description label"
      ),
      display.items.map { it.tip }
    )
    val textItems = display.items.filterIsInstance<XposedInfoItemDisplay.Text>()
    assertEquals(
      listOf(
        "93",
        "102",
        "Declared",
        "True",
        "JavaEntry",
        "native_entry",
        "LegacyEntry",
        "Description"
      ),
      textItems.map { it.text }
    )
    assertEquals(XposedInfoTextStyle.Body, textItems.last().textStyle)
    assertEquals(
      List(7) { XposedInfoTextStyle.Title },
      textItems.dropLast(1).map { it.textStyle }
    )
    val scopeItem = display.items.filterIsInstance<XposedInfoItemDisplay.ScopeApps>().single()
    assertEquals(
      listOf("com.example", "android", "com.missing"),
      scopeItem.apps.map { it.packageName }
    )
    assertEquals(
      listOf("Example", "Android System", "Missing"),
      scopeItem.apps.map { it.label }
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
        autoHotReloadDeclared = false,
        staticScope = false,
        defaultScope = emptyList(),
        javaInitClasses = null,
        nativeInitLibraries = null,
        legacyInitClass = null,
        description = null
      ),
      strings = strings
    )

    assertEquals(emptyList<Any>(), display.items)
  }

  @Test
  fun `normalizes scope while preserving declaration order`() {
    assertEquals(
      listOf("android", "*", "com.example"),
      normalizeXposedScope(listOf(" android ", "", "*", "android", " com.example "))
    )
  }

  @Test
  fun `normalizes duplicate wildcard scope`() {
    assertEquals(listOf("*"), normalizeXposedScope(listOf("*", " * ")))
  }

  @Test
  fun `renders wildcard scope as text instead of an app icon`() {
    val scopeItem = buildLiteralScopeItem("*")

    assertEquals("Default", scopeItem.tip)
    assertEquals("*", scopeItem.text)
  }

  @Test
  fun `renders system scope as text instead of an app icon`() {
    val scopeItem = buildLiteralScopeItem("system")

    assertEquals("Default", scopeItem.tip)
    assertEquals("system", scopeItem.text)
  }

  private fun buildLiteralScopeItem(scope: String): XposedInfoItemDisplay.Text {
    val display = buildXposedInfoBottomSheetDisplay(
      info = XposedModuleInfo(
        appName = "Module",
        settingsIntent = null,
        minVersion = null,
        targetVersion = null,
        autoHotReloadDeclared = false,
        staticScope = false,
        defaultScope = listOf(
          XposedScopeAppInfo(packageName = scope, label = scope, packageInfo = null)
        ),
        javaInitClasses = null,
        nativeInitLibraries = null,
        legacyInitClass = null,
        description = null
      ),
      strings = strings
    )

    return display.items.single() as XposedInfoItemDisplay.Text
  }

  private companion object {
    val strings = XposedInfoDisplayStrings(
      minVersion = "Minimum",
      targetVersion = "Target",
      autoHotReload = "Automatic hot reload",
      hotReloadDeclared = "Declared",
      staticScope = "Static",
      defaultScope = "Default",
      initClass = "Init",
      description = "Description label",
      trueValue = "True"
    )
  }
}
