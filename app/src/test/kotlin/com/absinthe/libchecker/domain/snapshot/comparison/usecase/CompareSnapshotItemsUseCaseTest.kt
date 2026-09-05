package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.utils.dex.DexEntryInfo
import com.absinthe.libchecker.utils.dex.ResourceEntryInfo
import com.absinthe.libchecker.utils.toJson
import kotlin.random.Random
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

  @Test
  fun duplicateNamesKeepFirstMatchAndMultiplicity() {
    val old = snapshotItem().copy(
      nativeLibs = listOf(LibStringItem("same", 10), LibStringItem("same", 20)).toJson().orEmpty(),
      metadata = listOf(LibStringItem("key", source = "first")).toJson().orEmpty()
    )
    val new = old.copy(
      nativeLibs = listOf(LibStringItem("same", 20)).toJson().orEmpty(),
      metadata = listOf(
        LibStringItem("key", source = "first"),
        LibStringItem("key", source = "second")
      ).toJson().orEmpty()
    )
    val diff = compare(old, new, emptySet())!!
    assertEquals(2, diff.changed)
    assertEquals(1, diff.removed)
    assertEquals(0, diff.added)
  }

  @Test
  fun componentMovesKeepFirstShortNameMatchForEveryNewComponent() {
    val old = snapshotItem().copy(services = listOf("a.Service", "b.Service").toJson().orEmpty())
    val new = old.copy(services = listOf("c.Service", "d.Service").toJson().orEmpty())
    val diff = compare(old, new, emptySet())!!
    assertEquals(2, diff.moved)
    assertEquals(1, diff.removed)
    assertEquals(0, diff.added)
  }

  @Test
  fun indexedNamesAreEquivalentToLegacyLoopAcrossDuplicateArchives() {
    val random = Random(7123)
    repeat(200) {
      val oldItems = List(random.nextInt(30)) {
        LibStringItem("lib${random.nextInt(8)}", random.nextLong(4))
      }
      val newItems = List(random.nextInt(30)) {
        LibStringItem("lib${random.nextInt(8)}", random.nextLong(4))
      }
      val remaining = oldItems.toMutableList()
      var changed = 0
      var added = 0
      for (item in newItems) {
        val old = oldItems.find { it.name == item.name }
        if (old == null) {
          added++
        } else {
          if (old.size != item.size) changed++
          remaining.remove(remaining.find { it.name == item.name })
        }
      }
      val old = snapshotItem().copy(nativeLibs = oldItems.toJson().orEmpty())
      val new = old.copy(nativeLibs = newItems.toJson().orEmpty())
      val diff = compare(old, new, emptySet())!!
      assertEquals(changed, diff.changed)
      assertEquals(added, diff.added)
      assertEquals(remaining.size, diff.removed)
    }
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
