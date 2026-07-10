package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.rulesbundle.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibStringComponentItemDisplayTest {

  @Test
  fun buildsDisplayWithMarkedRuleAndProcess() {
    val display = LibStringComponentItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(
          name = "com.example.SyncService",
          process = ":sync"
        ),
        rule = newRule(label = "Sync SDK")
      ),
      type = SERVICE,
      itemDisplayOptions = AdvancedOptions.SHOW_MARKED_LIB,
      processMode = true,
      processIndicatorColor = 0xff336699.toInt()
    )

    assertEquals(
      LibStringItemNameDisplay(
        text = "com.example.SyncService",
        decoration = LibStringItemNameDisplay.Decoration.Plain
      ),
      display.name
    )
    assertTrue(display.showRuleChip)
    assertEquals("Sync SDK", display.ruleLabel)
    assertEquals(":sync", display.processName)
    assertEquals(0xff336699.toInt(), display.processIndicatorColor)
    assertEquals(
      "com.example.SyncService, Sync SDK, :sync",
      display.contentDescription
    )
  }

  @Test
  fun hidesRuleLabelWhenMarkedRuleOptionIsOff() {
    val display = LibStringComponentItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(name = "com.example.SyncService"),
        rule = newRule(label = "Sync SDK")
      ),
      type = SERVICE,
      itemDisplayOptions = 0,
      processMode = false
    )

    assertFalse(display.showRuleChip)
    assertNull(display.ruleLabel)
    assertEquals("com.example.SyncService", display.contentDescription)
  }

  @Test
  fun skipsBlankProcessName() {
    val display = LibStringComponentItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(
          name = "com.example.SyncService",
          process = ""
        ),
        rule = null
      ),
      type = SERVICE,
      itemDisplayOptions = AdvancedOptions.SHOW_MARKED_LIB,
      processMode = true
    )

    assertNull(display.processName)
    assertEquals("com.example.SyncService", display.contentDescription)
  }

  @Test
  fun hidesRuleChipWithoutRule() {
    val display = LibStringComponentItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(name = "com.example.SyncService"),
        rule = null
      ),
      type = SERVICE,
      itemDisplayOptions = AdvancedOptions.SHOW_MARKED_LIB,
      processMode = false
    )

    assertFalse(display.showRuleChip)
    assertNull(display.ruleLabel)
  }

  @Test
  fun hidesProcessWhenProcessModeIsOff() {
    val display = LibStringComponentItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(
          name = "com.example.SyncService",
          process = ":sync"
        ),
        rule = null
      ),
      type = SERVICE,
      itemDisplayOptions = AdvancedOptions.SHOW_MARKED_LIB,
      processMode = false
    )

    assertNull(display.processName)
    assertNull(display.processIndicatorColor)
    assertEquals("com.example.SyncService", display.contentDescription)
  }

  private fun newRule(label: String): Rule {
    return Rule(
      "com.example.SyncService",
      SERVICE,
      label,
      42,
      "https://example.com",
      "com.example.*",
      false
    )
  }
}
