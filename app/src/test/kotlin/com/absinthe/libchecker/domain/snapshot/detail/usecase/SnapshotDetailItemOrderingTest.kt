package com.absinthe.libchecker.domain.snapshot.detail.usecase

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.visitKeyedSnapshotDiff
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.libchecker.utils.dex.DexEntryInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailItemOrderingTest {

  @Test
  fun `package entries use numeric order regardless of stored order`() {
    val expected = listOf("classes.dex", "classes2.dex", "classes9.dex", "classes10.dex", "classes12.dex")
    val storedOrders = listOf(
      expected.sorted(),
      expected.reversed(),
      listOf("classes9.dex", "classes12.dex", "classes.dex", "classes10.dex", "classes2.dex")
    )

    for (names in storedOrders) {
      for (status in listOf(ADDED, CHANGED, REMOVED)) {
        val items = names.map { item("base/$it", status) }

        assertEquals(
          expected.map { "base/$it" },
          orderSnapshotDetailSectionItems(items, DEX).map { it.name }
        )
        assertEquals(names.map { "base/$it" }, items.map { it.name })
      }
    }
  }

  @Test
  fun `mixed diff statuses from shuffled old snapshots are sorted together`() {
    val old = listOf(
      DexEntryInfo("base/classes12.dex", 12, 1, 1),
      DexEntryInfo("base/classes9.dex", 9, 1, 1),
      DexEntryInfo("base/classes2.dex", 2, 1, 1),
      DexEntryInfo("base/classes.dex", 1, 1, 1)
    )
    val new = listOf(
      DexEntryInfo("base/classes10.dex", 10, 1, 1),
      DexEntryInfo("base/classes2.dex", 20, 1, 2),
      DexEntryInfo("base/classes12.dex", 120, 1, 2)
    )
    val diffs = buildList {
      visitKeyedSnapshotDiff(old.associateBy { it.name }, new.associateBy { it.name }) { status, old, new ->
        add(item((new ?: requireNotNull(old)).name, status))
      }
    }
    val originalDiffs = diffs.toList()

    val ordered = orderSnapshotDetailSectionItems(diffs, DEX)

    assertEquals(
      listOf(
        "base/classes.dex" to REMOVED,
        "base/classes2.dex" to CHANGED,
        "base/classes9.dex" to REMOVED,
        "base/classes10.dex" to ADDED,
        "base/classes12.dex" to CHANGED
      ),
      ordered.map { it.name to it.diffType }
    )
    assertEquals(originalDiffs.toSet(), ordered.toSet())
    assertEquals(originalDiffs, diffs)
  }

  @Test
  fun `base and split identities remain distinct and resources stay at the end`() {
    val resourceNames = listOf(
      "split:feature10/resources.arsc",
      "base/resources.arsc",
      "split:feature2/resources.arsc"
    )
    val dexNames = listOf(
      "split:feature10/classes.dex",
      "split:feature2/classes10.dex",
      "base/classes10.dex",
      "split:feature2/classes2.dex",
      "base/classes2.dex",
      "split:feature2/classes.dex",
      "base/classes.dex"
    )
    val items = (dexNames + resourceNames).map { item(it) }

    val ordered = orderSnapshotDetailSectionItems(items, DEX)

    assertEquals(
      listOf(
        "base/classes.dex",
        "base/classes2.dex",
        "base/classes10.dex",
        "split:feature2/classes.dex",
        "split:feature2/classes2.dex",
        "split:feature2/classes10.dex",
        "split:feature10/classes.dex"
      ) + resourceNames,
      ordered.map { it.name }
    )
    assertEquals(items.toSet(), ordered.toSet())
  }

  @Test
  fun `legacy resources row follows dex entries`() {
    val resource = item("resources.arsc", CHANGED)
    val items = listOf(item("base/classes10.dex"), item("base/classes2.dex"), resource)

    assertEquals(
      listOf(items[1], items[0], resource),
      orderSnapshotDetailSectionItems(items, DEX)
    )
    assertEquals(listOf(resource), orderSnapshotDetailSectionItems(listOf(resource), DEX))
    assertEquals(emptyList<SnapshotDetailItem>(), orderSnapshotDetailSectionItems(emptyList(), DEX))
  }

  @Test
  fun `leading zeros break numeric ties and oversized numbers do not overflow`() {
    val expected = listOf(
      "base/classes.dex",
      "base/classes0.dex",
      "base/classes00.dex",
      "base/classes2.dex",
      "base/classes02.dex",
      "base/classes002.dex",
      "base/classes10.dex",
      "base/classes010.dex",
      "base/classes9223372036854775807.dex",
      "base/classes9223372036854775808.dex",
      "base/classes100000000000000000000.dex"
    )

    assertEquals(
      expected,
      orderSnapshotDetailSectionItems(expected.reversed().map { item(it) }, DEX).map { it.name }
    )
  }

  @Test
  fun `other sections retain their original order and items`() {
    val otherTypes = listOf(NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, PERMISSION, METADATA)
    val items = otherTypes.flatMap { type ->
      listOf(
        item("entry10", REMOVED, type),
        item("entry2", ADDED, type),
        item("entry1", CHANGED, type)
      )
    } + item("base/classes.dex")

    for (type in otherTypes) {
      assertEquals(items.filter { it.itemType == type }, orderSnapshotDetailSectionItems(items, type))
    }
  }

  private fun item(name: String, status: Int = ADDED, type: Int = DEX): SnapshotDetailItem {
    return SnapshotDetailItem(name, "Title: $name", "Details: $name", status, type)
  }
}
