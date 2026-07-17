package com.absinthe.libchecker.database.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LCItemTest {

  @Test
  fun `recognizes archived list item sentinel`() {
    assertTrue(item(LCItem.ARCHIVED_VERSION_NAME).isArchived)
    assertFalse(item("1.0").isArchived)
  }

  private fun item(versionName: String): LCItem {
    return LCItem(
      packageName = "com.example",
      label = "Example",
      versionName = versionName,
      versionCode = 1L,
      installedTime = 0L,
      lastUpdatedTime = 0L,
      isSystem = false,
      abi = 3,
      features = 0,
      targetApi = 35,
      variant = 0
    )
  }
}
