package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.fromJson

class CompareSnapshotItemsUseCase {

  operator fun invoke(
    oldInfo: SnapshotItem?,
    newInfo: SnapshotItem?,
    trackPackageNames: Set<String>
  ): SnapshotDiffItem? {
    if (oldInfo == null && newInfo == null) {
      return null
    } else if (newInfo == null || oldInfo == null) {
      val targetInfo = newInfo ?: oldInfo!!
      val newInstalled = newInfo != null
      return SnapshotDiffItem(
        targetInfo.packageName,
        targetInfo.lastUpdatedTime,
        SnapshotDiffItem.DiffNode(targetInfo.label),
        SnapshotDiffItem.DiffNode(targetInfo.versionName),
        SnapshotDiffItem.DiffNode(targetInfo.versionCode),
        SnapshotDiffItem.DiffNode(targetInfo.abi),
        SnapshotDiffItem.DiffNode(targetInfo.targetApi),
        SnapshotDiffItem.DiffNode(targetInfo.compileSdk),
        SnapshotDiffItem.DiffNode(targetInfo.minSdk),
        SnapshotDiffItem.DiffNode(targetInfo.nativeLibs),
        SnapshotDiffItem.DiffNode(targetInfo.services),
        SnapshotDiffItem.DiffNode(targetInfo.activities),
        SnapshotDiffItem.DiffNode(targetInfo.receivers),
        SnapshotDiffItem.DiffNode(targetInfo.providers),
        SnapshotDiffItem.DiffNode(targetInfo.permissions),
        SnapshotDiffItem.DiffNode(targetInfo.metadata),
        SnapshotDiffItem.DiffNode(targetInfo.packageSize),
        newInstalled = newInstalled,
        deleted = !newInstalled,
        isTrackItem = targetInfo.packageName in trackPackageNames
      )
    } else {
      return SnapshotDiffItem(
        packageName = newInfo.packageName,
        updateTime = newInfo.lastUpdatedTime,
        labelDiff = SnapshotDiffItem.DiffNode(oldInfo.label, newInfo.label),
        versionNameDiff = SnapshotDiffItem.DiffNode(oldInfo.versionName, newInfo.versionName),
        versionCodeDiff = SnapshotDiffItem.DiffNode(oldInfo.versionCode, newInfo.versionCode),
        abiDiff = SnapshotDiffItem.DiffNode(oldInfo.abi, newInfo.abi),
        targetApiDiff = SnapshotDiffItem.DiffNode(oldInfo.targetApi, newInfo.targetApi),
        compileSdkDiff = SnapshotDiffItem.DiffNode(oldInfo.compileSdk, newInfo.compileSdk),
        minSdkDiff = SnapshotDiffItem.DiffNode(oldInfo.minSdk, newInfo.minSdk),
        nativeLibsDiff = SnapshotDiffItem.DiffNode(oldInfo.nativeLibs, newInfo.nativeLibs),
        servicesDiff = SnapshotDiffItem.DiffNode(oldInfo.services, newInfo.services),
        activitiesDiff = SnapshotDiffItem.DiffNode(oldInfo.activities, newInfo.activities),
        receiversDiff = SnapshotDiffItem.DiffNode(oldInfo.receivers, newInfo.receivers),
        providersDiff = SnapshotDiffItem.DiffNode(oldInfo.providers, newInfo.providers),
        permissionsDiff = SnapshotDiffItem.DiffNode(oldInfo.permissions, newInfo.permissions),
        metadataDiff = SnapshotDiffItem.DiffNode(oldInfo.metadata, newInfo.metadata),
        packageSizeDiff = SnapshotDiffItem.DiffNode(oldInfo.packageSize, newInfo.packageSize),
        isTrackItem = newInfo.packageName in trackPackageNames
      ).apply {
        val diffIndicator = compareDiffIndicator(this)
        added = diffIndicator.added
        removed = diffIndicator.removed
        changed = diffIndicator.changed
        moved = diffIndicator.moved
      }
    }
  }

  private fun compareDiffIndicator(item: SnapshotDiffItem): DiffIndicator {
    val native = compareNativeDiff(
      item.nativeLibsDiff.old.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      ) ?: emptyList(),
      item.nativeLibsDiff.new?.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      )
    )
    val services = compareComponentsDiff(item.servicesDiff)
    val activities = compareComponentsDiff(item.activitiesDiff)
    val receivers = compareComponentsDiff(item.receiversDiff)
    val providers = compareComponentsDiff(item.providersDiff)
    val permissions = comparePermissionsDiff(
      item.permissionsDiff.old.fromJson<List<String>>(
        List::class.java,
        String::class.java
      ).orEmpty().toSet(),
      item.permissionsDiff.new?.fromJson<List<String>>(
        List::class.java,
        String::class.java
      )?.toSet()
    )
    val metadata = compareMetadataDiff(
      item.metadataDiff.old.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      ) ?: emptyList(),
      item.metadataDiff.new?.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      )
    )

    return DiffIndicator().apply {
      added =
        native.added or services.added or activities.added or receivers.added or providers.added or permissions.added or metadata.added
      removed =
        native.removed or services.removed or activities.removed or receivers.removed or providers.removed or permissions.removed or metadata.removed
      changed =
        native.changed or metadata.changed
      moved =
        services.moved or activities.moved or receivers.moved or providers.moved
    }
  }

  private fun compareNativeDiff(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): DiffIndicator {
    if (newList == null) {
      return DiffIndicator(removed = Int.MAX_VALUE)
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val node = DiffIndicator()

    val iterator = tempNewList.iterator()
    var nextItem: LibStringItem

    while (iterator.hasNext()) {
      nextItem = iterator.next()
      oldList.find { it.name == nextItem.name }?.let {
        if (it.size != nextItem.size) {
          node.changed += 1
        }
        iterator.remove()
        tempOldList.remove(tempOldList.find { item -> item.name == nextItem.name })
      }
    }

    if (tempOldList.isNotEmpty()) {
      node.removed = tempOldList.size
    }
    if (tempNewList.isNotEmpty()) {
      node.added = tempNewList.size
    }
    return node
  }

  private fun compareComponentsDiff(diffNode: SnapshotDiffItem.DiffNode<String>): DiffIndicator {
    if (diffNode.new == null) {
      return DiffIndicator(removed = Int.MAX_VALUE)
    }

    val oldSet = diffNode.old.fromJson<List<String>>(
      List::class.java,
      String::class.java
    ).orEmpty().toSet()
    val newSet = diffNode.new.fromJson<List<String>>(
      List::class.java,
      String::class.java
    ).orEmpty().toSet()

    val removeList = (oldSet - newSet).toMutableSet()
    val addList = (newSet - oldSet).toMutableSet()
    val node = DiffIndicator()
    val pendingRemovedOldSet = mutableSetOf<String>()
    val pendingRemovedNewSet = mutableSetOf<String>()

    for (item in addList) {
      removeList.find { it.substringAfterLast(".") == item.substringAfterLast(".") }?.let {
        node.moved += 1
        pendingRemovedOldSet += it
        pendingRemovedNewSet += item
      }
    }
    removeList.removeAll(pendingRemovedOldSet)
    addList.removeAll(pendingRemovedNewSet)

    if (removeList.isNotEmpty()) {
      node.removed = removeList.size
    }
    if (addList.isNotEmpty()) {
      node.added = addList.size
    }
    return node
  }

  private fun comparePermissionsDiff(
    oldSet: Set<String>,
    newSet: Set<String>?
  ): DiffIndicator {
    if (newSet == null) {
      return DiffIndicator(removed = Int.MAX_VALUE)
    }

    val removeList = oldSet - newSet
    val addList = newSet - oldSet
    val node = DiffIndicator()

    if (removeList.isNotEmpty()) {
      node.removed = removeList.size
    }
    if (addList.isNotEmpty()) {
      node.added = addList.size
    }
    return node
  }

  private fun compareMetadataDiff(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): DiffIndicator {
    if (newList == null) {
      return DiffIndicator(removed = Int.MAX_VALUE)
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val node = DiffIndicator()

    val iterator = tempNewList.iterator()
    var nextItem: LibStringItem

    while (iterator.hasNext()) {
      nextItem = iterator.next()
      oldList.find { it.name == nextItem.name }?.let {
        if (it.source != nextItem.source) {
          node.changed += 1
        }
        iterator.remove()
        tempOldList.remove(tempOldList.find { item -> item.name == nextItem.name })
      }
    }

    if (tempOldList.isNotEmpty()) {
      node.removed = tempOldList.size
    }
    if (tempNewList.isNotEmpty()) {
      node.added = tempNewList.size
    }
    return node
  }

  private data class DiffIndicator(
    var added: Int = 0,
    var removed: Int = 0,
    var changed: Int = 0,
    var moved: Int = 0
  )
}
