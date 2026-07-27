package com.absinthe.libchecker.domain.snapshot.list.capture

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.database.entity.SnapshotItem
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@Suppress("DEPRECATION")
class CaptureInstalledSnapshotReuseTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun currentFailedStatsAreRecollected() {
    val packageInfo = packageInfo()
    val snapshot = snapshotItem(packageInfo).copy(
      statsVersion = SnapshotItem.CURRENT_STATS_VERSION,
      dexStatsAvailable = false,
      resourceStatsAvailable = false
    )

    assertFalse(snapshot.isReusableForCapture(packageInfo, shouldSaveFullSnapshot = false))
  }

  @Test
  fun legacyStatsAreRecollected() {
    val packageInfo = packageInfo()
    val snapshot = snapshotItem(packageInfo).copy(statsVersion = 0)

    assertFalse(snapshot.isReusableForCapture(packageInfo, shouldSaveFullSnapshot = false))
  }

  private fun packageInfo(): PackageInfo {
    val apk = temporaryFolder.newFile("base.apk").apply {
      writeBytes(byteArrayOf(1, 2, 3))
    }
    return PackageInfo().apply {
      packageName = "com.example"
      versionCode = 1
      lastUpdateTime = 2
      applicationInfo = ApplicationInfo().apply {
        sourceDir = apk.path
      }
    }
  }

  private fun snapshotItem(packageInfo: PackageInfo): SnapshotItem {
    return SnapshotItem(
      id = null,
      packageName = packageInfo.packageName,
      timeStamp = 1,
      label = "Example",
      versionName = "1",
      versionCode = packageInfo.versionCode.toLong(),
      isArchived = false,
      installedTime = 1,
      lastUpdatedTime = packageInfo.lastUpdateTime,
      isSystem = false,
      abi = 0,
      targetApi = 35,
      nativeLibs = "[]",
      services = "[]",
      activities = "[]",
      receivers = "[]",
      providers = "[]",
      permissions = "[]",
      metadata = "[]",
      packageSize = packageInfo.applicationInfo?.sourceDir?.let(::File)?.length() ?: 0,
      compileSdk = 35,
      minSdk = 24
    )
  }
}
