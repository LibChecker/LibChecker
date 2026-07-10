package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.rulesbundle.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibStringNativeItemDisplayTest {

  @Test
  fun buildsCompleteNativeRowDisplay() {
    val rule = Rule(
      "libsample.so",
      NATIVE,
      "Sample SDK",
      42,
      null,
      null,
      false
    )
    val display = LibStringNativeItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(name = "libsample.so", size = 1024L),
        rule = rule,
        nativeDisplayData = NativeLibraryItemDisplayData(
          sizeText = "1 KB",
          labels = listOf("16 KB", "ZIPALIGN")
        )
      ),
      itemDisplayOptions = AdvancedOptions.SHOW_MARKED_LIB
    )

    assertEquals("libsample.so", display.name.text)
    assertEquals("1 KB", display.sizeText)
    assertEquals(listOf("16 KB", "ZIPALIGN"), display.labels)
    assertEquals(rule, display.rule)
    assertEquals("libsample.so, 1 KB 16 KB  ZIPALIGN, Sample SDK", display.contentDescription)
  }

  @Test
  fun hidesRuleWhenMarkedLibraryOptionIsOff() {
    val display = LibStringNativeItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(name = "libsample.so"),
        rule = Rule("libsample.so", NATIVE, "Sample SDK", 42, null, null, false),
        nativeDisplayData = NativeLibraryItemDisplayData("0 B", emptyList())
      ),
      itemDisplayOptions = 0
    )

    assertNull(display.rule)
    assertEquals("libsample.so, 0 B", display.contentDescription)
  }
}
