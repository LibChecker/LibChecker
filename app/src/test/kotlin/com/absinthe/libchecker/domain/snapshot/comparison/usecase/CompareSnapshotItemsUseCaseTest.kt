package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.utils.dex.DexEntryInfo
import com.absinthe.libchecker.utils.dex.ResourceEntryInfo
import com.absinthe.libchecker.utils.toJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareSnapshotItemsUseCaseTest {

  private val compare = CompareSnapshotItemsUseCase()

  @Test
  fun dexOnlyChangeIsReported() {
    val oldItem = snapshotItem(
      dexInfo = listOf(DexEntryInfo("base/classes.dex", 10, 1, 10)).toJson().orEmpty()
    )
    val newItem = snapshotItem(
      dexInfo = listOf(DexEntryInfo("base/classes.dex", 20, 2, 20)).toJson().orEmpty()
    )

    val diff = compare(oldItem, newItem, emptySet())!!

    assertEquals(1, diff.changed)
    assertFalse(diff.isNothingChanged())
  }

  @Test
  fun sameSizeDexContentChangeIsReported() {
    val oldItem = snapshotItem(
      dexInfo = listOf(DexEntryInfo("base/classes.dex", 10, 1, 1)).toJson().orEmpty()
    )
    val newItem = snapshotItem(
      dexInfo = listOf(DexEntryInfo("base/classes.dex", 10, 1, 2)).toJson().orEmpty()
    )

    val diff = compare(oldItem, newItem, emptySet())!!

    assertEquals(1, diff.changed)
    assertFalse(diff.isNothingChanged())
  }

  @Test
  fun resourcesOnlyChangeIsReported() {
    val oldItem = snapshotItem(resourcesSize = 10)
    val newItem = snapshotItem(resourcesSize = 20)

    val diff = compare(oldItem, newItem, emptySet())!!

    assertEquals(1, diff.changed)
    assertFalse(diff.isNothingChanged())
  }

  @Test
  fun dexAndResourcesChangesAreCountedSeparately() {
    val oldItem = snapshotItem(
      dexInfo = listOf(DexEntryInfo("base/classes.dex", 10, 1, 10)).toJson().orEmpty(),
      resourcesSize = 10
    )
    val newItem = snapshotItem(
      dexInfo = listOf(DexEntryInfo("base/classes.dex", 20, 2, 20)).toJson().orEmpty(),
      resourcesSize = 20
    )

    val diff = compare(oldItem, newItem, emptySet())!!

    assertEquals(2, diff.changed)
  }

  @Test
  fun missingLegacyStatsAreNotReportedAsEmptyStats() {
    val oldItem = snapshotItem(
      dexInfo = "[]",
      resourcesSize = 0,
      statsVersion = 0
    )
    val newItem = snapshotItem(
      dexInfo = listOf(DexEntryInfo("base/classes.dex", 20, 2, 20)).toJson().orEmpty(),
      resourcesSize = 20
    )

    val diff = compare(oldItem, newItem, emptySet())!!

    assertTrue(diff.isNothingChanged())
    assertNull(diff.dexInfoDiff.new)
    assertNull(diff.resourcesSizeDiff.new)
  }

  @Test
  fun sameSizeResourceContentChangeIsReported() {
    val oldItem = snapshotItem(
      resourcesSize = 10,
      resourceInfo = listOf(ResourceEntryInfo("base/resources.arsc", 10, 1))
        .toJson().orEmpty()
    )
    val newItem = snapshotItem(
      resourcesSize = 10,
      resourceInfo = listOf(ResourceEntryInfo("base/resources.arsc", 10, 2))
        .toJson().orEmpty()
    )

    val diff = compare(oldItem, newItem, emptySet())!!

    assertEquals(1, diff.changed)
    assertFalse(diff.isNothingChanged())
  }

  @Test
  fun offsettingSplitResourceSizeChangesAreReported() {
    val oldItem = snapshotItem(
      resourcesSize = 30,
      resourceInfo = listOf(
        ResourceEntryInfo("base/resources.arsc", 10, 1),
        ResourceEntryInfo("split:feature/resources.arsc", 20, 2)
      ).toJson().orEmpty()
    )
    val newItem = snapshotItem(
      resourcesSize = 30,
      resourceInfo = listOf(
        ResourceEntryInfo("base/resources.arsc", 15, 3),
        ResourceEntryInfo("split:feature/resources.arsc", 15, 4)
      ).toJson().orEmpty()
    )

    val diff = compare(oldItem, newItem, emptySet())!!

    assertEquals(2, diff.changed)
    assertFalse(diff.isNothingChanged())
  }

  private fun snapshotItem(
    dexInfo: String = "[]",
    resourcesSize: Long = 0,
    resourceInfo: String = if (resourcesSize == 0L) {
      "[]"
    } else {
      listOf(ResourceEntryInfo("base/resources.arsc", resourcesSize, resourcesSize))
        .toJson().orEmpty()
    },
    statsVersion: Int = SnapshotItem.CURRENT_STATS_VERSION
  ): SnapshotItem {
    return SnapshotItem(
      id = null,
      packageName = "com.example",
      timeStamp = 1,
      label = "Example",
      versionName = "1",
      versionCode = 1,
      isArchived = false,
      installedTime = 1,
      lastUpdatedTime = 1,
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
      packageSize = 1,
      compileSdk = 35,
      minSdk = 24,
      dexInfo = dexInfo,
      resourceInfo = resourceInfo,
      resourcesSize = resourcesSize,
      statsVersion = statsVersion,
      dexStatsAvailable = statsVersion == SnapshotItem.CURRENT_STATS_VERSION,
      resourceStatsAvailable = statsVersion == SnapshotItem.CURRENT_STATS_VERSION
    )
  }
}
