package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.utils.toJson
import com.absinthe.rulesbundle.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibStringStaticItemDisplayTest {

  @Test
  fun parsesStaticLibraryDetailIntoSemanticFields() {
    val rule = Rule("sample", STATIC, "Sample SDK", 42, null, null, false)
    val source = StaticLibItem(
      name = "sample",
      version = 12,
      certDigest = "AA:BB",
      path = "/system/framework/sample.jar"
    ).toJson()
    val display = LibStringStaticItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(name = "sample", source = source),
        rule = rule
      ),
      itemDisplayOptions = AdvancedOptions.SHOW_MARKED_LIB
    )

    assertEquals("/system/framework/sample.jar", display.detail?.path)
    assertEquals(12, display.detail?.versionCode)
    assertEquals("AA:BB", display.detail?.certificateDigest)
    assertEquals(rule, display.rule)
    assertEquals(
      "sample, [Path] /system/framework/sample.jar\n[Version Code] 12\n[Cert] AA:BB, Sample SDK",
      display.contentDescription
    )
  }

  @Test
  fun invalidDetailClearsStaticRowContent() {
    val display = LibStringStaticItemDisplay.create(
      item = LibStringItemChip(
        item = LibStringItem(name = "sample", source = "not-json"),
        rule = null
      ),
      itemDisplayOptions = 0
    )

    assertNull(display.detail)
    assertEquals("sample", display.contentDescription)
  }
}
