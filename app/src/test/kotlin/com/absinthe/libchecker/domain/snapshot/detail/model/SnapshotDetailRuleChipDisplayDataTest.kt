package com.absinthe.libchecker.domain.snapshot.detail.model

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.rulesbundle.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotDetailRuleChipDisplayDataTest {

  @Test
  fun mapsRuleToChipDisplayData() {
    val rule = Rule(
      "libfoo.so",
      NATIVE,
      "Foo SDK",
      42,
      "https://example.com",
      "libfoo.*",
      false
    )

    assertEquals(
      SnapshotDetailRuleChipDisplayData(
        label = "Foo SDK",
        iconRes = 42,
        regexName = "libfoo.*",
        isSimpleColorIcon = false,
        useColorfulIcon = true
      ),
      buildSnapshotDetailRuleChipDisplayData(rule, colorfulRuleIcon = true)
    )
  }

  @Test
  fun returnsNullWithoutRule() {
    assertNull(buildSnapshotDetailRuleChipDisplayData(null, colorfulRuleIcon = true))
  }
}
