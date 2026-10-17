package com.absinthe.libchecker.domain.snapshot.detail.usecase

import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem

internal fun orderSnapshotDetailSectionItems(
  items: List<SnapshotDetailItem>,
  @LibType type: Int
): List<SnapshotDetailItem> {
  val sectionItems = items.filter { it.itemType == type }
  if (type != DEX) return sectionItems

  // Resource rows follow all DEX rows, including resources from split APKs.
  val (resources, dex) = sectionItems.partition { it.name.substringAfterLast('/') == "resources.arsc" }
  return dex.sortedWith { left, right -> comparePackageEntryNames(left.name, right.name) } + resources
}

private val packageEntryNameParts = Regex("[0-9]+|[^0-9]")

private fun comparePackageEntryNames(left: String, right: String): Int {
  val leftParts = packageEntryNameParts.findAll(left).iterator()
  val rightParts = packageEntryNameParts.findAll(right).iterator()
  while (leftParts.hasNext() && rightParts.hasNext()) {
    val leftPart = leftParts.next().value
    val rightPart = rightParts.next().value
    val comparison = if (leftPart.first() in '0'..'9' && rightPart.first() in '0'..'9') {
      // Compare digit counts and text to avoid overflow, even for imported names.
      val leftNumber = leftPart.trimStart('0')
      val rightNumber = rightPart.trimStart('0')
      compareValues(leftNumber.length, rightNumber.length)
        .takeIf { it != 0 }
        ?: leftNumber.compareTo(rightNumber).takeIf { it != 0 }
        ?: compareValues(leftPart.length, rightPart.length)
    } else {
      leftPart.compareTo(rightPart)
    }
    if (comparison != 0) return comparison
  }
  return compareValues(leftParts.hasNext(), rightParts.hasNext())
}
