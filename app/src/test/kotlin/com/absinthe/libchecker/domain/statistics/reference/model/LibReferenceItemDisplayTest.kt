package com.absinthe.libchecker.domain.statistics.reference.model

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.rulesbundle.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibReferenceItemDisplayTest {

  @Test
  fun buildsMarkedReferenceDisplay() {
    val rule = Rule(
      "libsample.so",
      NATIVE,
      "Sample SDK",
      42,
      null,
      null,
      false
    )

    val display = LibReferenceItemDisplay.create(
      reference = LibReference(
        libName = "libsample.so",
        rule = rule,
        referredList = setOf("one", "two"),
        type = NATIVE
      ),
      colorfulRuleIcon = false,
      notMarkedLabel = "Not marked",
      countText = "2"
    )

    assertEquals("Sample SDK", display.label)
    assertFalse(display.italicLabel)
    assertEquals("libsample.so", display.libName)
    assertEquals("2", display.count)
    assertEquals(42, display.iconRes)
    assertEquals("Sample SDK", display.iconContentDescription)
    assertEquals(!rule.isSimpleColorIcon, display.desaturateIcon)
    assertTrue(display.canOpenDetail)
    assertEquals("Sample SDK, libsample.so, 2", display.contentDescription)
  }

  @Test
  fun buildsUnmarkedAndroidPermissionDisplay() {
    val display = LibReferenceItemDisplay.create(
      reference = LibReference(
        libName = "android.permission.CAMERA",
        rule = null,
        referredList = setOf("one"),
        type = PERMISSION
      ),
      colorfulRuleIcon = true,
      notMarkedLabel = "Not marked",
      countText = "1"
    )

    assertEquals("Not marked", display.label)
    assertTrue(display.italicLabel)
    assertEquals(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android, display.iconRes)
    assertEquals("android.permission.CAMERA", display.iconContentDescription)
    assertFalse(display.desaturateIcon)
    assertFalse(display.canOpenDetail)
    assertEquals(
      "Not marked, android.permission.CAMERA, 1",
      display.contentDescription
    )
  }

  @Test
  fun buildsPackageGroupDisplayWithWildcardSuffix() {
    val display = MultipleAppsIconItemDisplay.create(
      reference = LibReference(
        libName = "com.example",
        rule = null,
        referredList = setOf("one", "two"),
        type = PACKAGE
      ),
      notMarkedLabel = "Not marked"
    )

    assertEquals("Not marked", display.label)
    assertEquals("com.example.*", display.libName)
    assertEquals("2", display.count)
    assertTrue(display.iconPackages.isEmpty())
    assertEquals("Not marked, com.example.*, 2", display.contentDescription)
  }
}
