package com.absinthe.libchecker.domain.app.list.model

import android.content.pm.PackageInfo
import com.absinthe.libchecker.constant.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListItemIconDisplayTest {

  @Test
  fun loadsPackageIconForInstalledApp() {
    val packageInfo = PackageInfo()

    val display = AppListItemIconDisplay.create(
      packageName = "com.absinthe.libchecker",
      packageInfo = packageInfo
    )

    assertTrue(display.usePackageIcon)
    assertSame(packageInfo, display.packageInfo)
  }

  @Test
  fun usesPlaceholderForExampleItem() {
    val display = AppListItemIconDisplay.create(
      packageName = Constants.EXAMPLE_PACKAGE,
      packageInfo = PackageInfo()
    )

    assertFalse(display.usePackageIcon)
    assertNull(display.packageInfo)
  }

  @Test
  fun usesPlaceholderWhenPackageInfoIsMissing() {
    val display = AppListItemIconDisplay.create(
      packageName = "com.example.missing",
      packageInfo = null
    )

    assertFalse(display.usePackageIcon)
    assertNull(display.packageInfo)
  }
}
