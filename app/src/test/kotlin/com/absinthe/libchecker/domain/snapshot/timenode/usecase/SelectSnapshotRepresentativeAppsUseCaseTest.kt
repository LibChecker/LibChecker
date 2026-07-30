package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectSnapshotRepresentativeAppsUseCaseTest {

  private val selectRepresentativeApps = SelectSnapshotRepresentativeAppsUseCase()

  @Test
  fun selectsOneRepresentativeFromEachChangeTierBeforeFillingRemainingSlots() {
    val previousItems = listOf(
      snapshotItem("tracked", lastUpdatedTime = 1),
      snapshotItem("version", versionCode = 1),
      snapshotItem("structure", permissions = "[]"),
      snapshotItem("metadata", packageSize = 1),
      snapshotItem("stable")
    )
    val currentItems = listOf(
      snapshotItem("tracked", lastUpdatedTime = 2),
      snapshotItem("version", versionCode = 2),
      snapshotItem("structure", permissions = "[\"camera\"]"),
      snapshotItem("metadata", packageSize = 2),
      snapshotItem("stable"),
      snapshotItem("new")
    )

    val result = selectRepresentativeApps(
      previousItems = previousItems,
      currentItems = currentItems,
      trackPackageNames = setOf("tracked")
    )

    assertEquals(
      listOf("tracked", "new", "version", "structure", "metadata"),
      result.take(5)
    )
  }

  @Test
  fun aSingleChangedAppStillProducesOneCandidate() {
    val previousItems = listOf(snapshotItem("changed", versionCode = 1))
    val currentItems = listOf(snapshotItem("changed", versionCode = 2))

    val result = selectRepresentativeApps(previousItems, currentItems, emptySet())

    assertEquals(listOf("changed"), result)
  }

  @Test
  fun limitsSystemAppsWhileUserCandidatesAreAvailable() {
    val previousItems = listOf(snapshotItem("anchor"))
    val currentItems = buildList {
      add(snapshotItem("anchor"))
      repeat(3) { index ->
        add(snapshotItem("user.$index", lastUpdatedTime = 100L - index))
      }
      repeat(10) { index ->
        add(
          snapshotItem(
            packageName = "system.$index",
            lastUpdatedTime = 200L - index,
            isSystem = true
          )
        )
      }
    }

    val result = selectRepresentativeApps(previousItems, currentItems, emptySet())
    val visibleItems = result.take(5)

    assertEquals(5, visibleItems.size)
    assertEquals(2, visibleItems.count { it.startsWith("system.") })
  }

  @Test
  fun baselineMixesRecentlyUpdatedAndLargeUserApps() {
    val currentItems = listOf(
      snapshotItem("recent.1", lastUpdatedTime = 100, packageSize = 1),
      snapshotItem("recent.2", lastUpdatedTime = 90, packageSize = 2),
      snapshotItem("recent.3", lastUpdatedTime = 80, packageSize = 3),
      snapshotItem("large.1", lastUpdatedTime = 70, packageSize = 100),
      snapshotItem("large.2", lastUpdatedTime = 60, packageSize = 90)
    )

    val result = selectRepresentativeApps(emptyList(), currentItems, emptySet())

    assertEquals(
      listOf("recent.1", "recent.2", "recent.3", "large.1", "large.2"),
      result.take(5)
    )
  }

  @Test
  fun unrelatedSnapshotsFallBackToLandmarks() {
    val previousItems = listOf(
      snapshotItem("shared", versionCode = 1),
      snapshotItem("old.1"),
      snapshotItem("old.2")
    )
    val currentItems = listOf(
      snapshotItem("shared", versionCode = 2, lastUpdatedTime = 1),
      snapshotItem("landmark", lastUpdatedTime = 100),
      snapshotItem("new.1", lastUpdatedTime = 50)
    )

    val result = selectRepresentativeApps(previousItems, currentItems, emptySet())

    assertEquals("landmark", result.first())
    assertTrue(result.contains("shared"))
  }

  private fun snapshotItem(
    packageName: String,
    versionCode: Long = 1,
    lastUpdatedTime: Long = 1,
    isSystem: Boolean = false,
    permissions: String = "[]",
    packageSize: Long = 1
  ): SnapshotItem {
    return SnapshotItem(
      id = null,
      packageName = packageName,
      timeStamp = 1,
      label = packageName,
      versionName = versionCode.toString(),
      versionCode = versionCode,
      isArchived = false,
      installedTime = 1,
      lastUpdatedTime = lastUpdatedTime,
      isSystem = isSystem,
      abi = 0,
      targetApi = 35,
      nativeLibs = "[]",
      services = "[]",
      activities = "[]",
      receivers = "[]",
      providers = "[]",
      permissions = permissions,
      metadata = "[]",
      packageSize = packageSize,
      compileSdk = 35,
      minSdk = 24
    )
  }
}
