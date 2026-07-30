package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import kotlin.math.abs

class SelectSnapshotRepresentativeAppsUseCase {

  operator fun invoke(
    previousItems: List<SnapshotItem>,
    currentItems: List<SnapshotItem>,
    trackPackageNames: Set<String>
  ): List<String> {
    if (currentItems.isEmpty()) {
      return emptyList()
    }
    if (!isComparable(previousItems, currentItems)) {
      return selectLandmarks(currentItems, trackPackageNames)
    }

    val previousByPackageName = previousItems.associateBy(SnapshotItem::packageName)
    val candidates = currentItems.mapNotNull { currentItem ->
      buildCandidate(
        previousItem = previousByPackageName[currentItem.packageName],
        currentItem = currentItem,
        isTracked = currentItem.packageName in trackPackageNames
      )
    }
    if (candidates.isEmpty()) {
      return selectLandmarks(currentItems, trackPackageNames)
    }

    val sortedCandidates = candidates.sortedWith(candidateComparator)
    val diverseCandidates = CandidateTier.entries.mapNotNull { tier ->
      sortedCandidates.firstOrNull { it.tier == tier }
    }
    val orderedCandidates = (diverseCandidates + sortedCandidates)
      .distinctBy { it.item.packageName }
    return takeWithSystemAppLimit(orderedCandidates.map(Candidate::item))
      .map(SnapshotItem::packageName)
  }

  private fun isComparable(
    previousItems: List<SnapshotItem>,
    currentItems: List<SnapshotItem>
  ): Boolean {
    if (previousItems.isEmpty()) {
      return false
    }
    val previousPackageNames = previousItems.asSequence()
      .map(SnapshotItem::packageName)
      .toSet()
    val currentPackageNames = currentItems.asSequence()
      .map(SnapshotItem::packageName)
      .toSet()
    val smallerSnapshotSize = minOf(previousPackageNames.size, currentPackageNames.size)
    if (smallerSnapshotSize == 0) {
      return false
    }
    val overlapCount = previousPackageNames.count(currentPackageNames::contains)
    return overlapCount.toDouble() / smallerSnapshotSize >= COMPARABLE_OVERLAP_RATIO
  }

  private fun buildCandidate(
    previousItem: SnapshotItem?,
    currentItem: SnapshotItem,
    isTracked: Boolean
  ): Candidate? {
    if (previousItem == null) {
      return Candidate(
        item = currentItem,
        tier = if (isTracked) CandidateTier.TRACKED else CandidateTier.NEW_INSTALL,
        changeBreadth = 1,
        packageSizeDelta = currentItem.packageSize
      )
    }

    val versionOrEnvironmentChanges = listOf(
      previousItem.versionName != currentItem.versionName,
      previousItem.versionCode != currentItem.versionCode,
      previousItem.isArchived != currentItem.isArchived,
      previousItem.abi != currentItem.abi,
      previousItem.targetApi != currentItem.targetApi,
      previousItem.compileSdk != currentItem.compileSdk,
      previousItem.minSdk != currentItem.minSdk
    )
    val structuralChanges = listOf(
      previousItem.nativeLibs != currentItem.nativeLibs,
      previousItem.services != currentItem.services,
      previousItem.activities != currentItem.activities,
      previousItem.receivers != currentItem.receivers,
      previousItem.providers != currentItem.providers,
      previousItem.permissions != currentItem.permissions,
      previousItem.metadata != currentItem.metadata,
      previousItem.hasDexStats() &&
        currentItem.hasDexStats() &&
        previousItem.dexInfo != currentItem.dexInfo,
      previousItem.hasResourceStats() &&
        currentItem.hasResourceStats() &&
        (
          previousItem.resourceInfo != currentItem.resourceInfo ||
            previousItem.resourcesSize != currentItem.resourcesSize
          )
    )
    val metadataChanges = listOf(
      previousItem.label != currentItem.label,
      previousItem.lastUpdatedTime != currentItem.lastUpdatedTime,
      previousItem.packageSize != currentItem.packageSize
    )
    val changeBreadth = versionOrEnvironmentChanges.count { it } +
      structuralChanges.count { it } +
      metadataChanges.count { it }
    if (changeBreadth == 0) {
      return null
    }

    val tier = when {
      isTracked -> CandidateTier.TRACKED
      versionOrEnvironmentChanges.any { it } -> CandidateTier.VERSION_OR_ENVIRONMENT
      structuralChanges.any { it } -> CandidateTier.STRUCTURAL
      else -> CandidateTier.METADATA
    }
    return Candidate(
      item = currentItem,
      tier = tier,
      changeBreadth = changeBreadth,
      packageSizeDelta = abs(currentItem.packageSize - previousItem.packageSize)
    )
  }

  private fun selectLandmarks(
    currentItems: List<SnapshotItem>,
    trackPackageNames: Set<String>
  ): List<String> {
    val trackedItems = currentItems
      .filter { it.packageName in trackPackageNames }
      .sortedWith(landmarkComparator)
    val userItems = currentItems
      .filterNot(SnapshotItem::isSystem)
    val recentUserItems = userItems
      .sortedWith(
        compareByDescending<SnapshotItem>(SnapshotItem::lastUpdatedTime)
          .thenBy(SnapshotItem::packageName)
      )
    val largeUserItems = userItems
      .sortedWith(
        compareByDescending<SnapshotItem>(SnapshotItem::packageSize)
          .thenBy(SnapshotItem::packageName)
      )
    val systemItems = currentItems
      .filter(SnapshotItem::isSystem)
      .sortedWith(landmarkComparator)
    val orderedItems = (
      trackedItems +
        recentUserItems.take(BASELINE_RECENT_APP_LIMIT) +
        largeUserItems.take(BASELINE_LARGE_APP_LIMIT) +
        recentUserItems +
        largeUserItems +
        systemItems
      ).distinctBy(SnapshotItem::packageName)
    return takeWithSystemAppLimit(orderedItems)
      .map(SnapshotItem::packageName)
  }

  private fun takeWithSystemAppLimit(orderedItems: List<SnapshotItem>): List<SnapshotItem> {
    val selectedItems = mutableListOf<SnapshotItem>()
    var selectedSystemAppCount = 0

    orderedItems.forEach { item ->
      if (selectedItems.size == CANDIDATE_LIMIT) {
        return@forEach
      }
      if (item.isSystem && selectedSystemAppCount >= SYSTEM_APP_LIMIT) {
        return@forEach
      }
      selectedItems.add(item)
      if (item.isSystem) {
        selectedSystemAppCount++
      }
    }
    if (selectedItems.size < CANDIDATE_LIMIT) {
      orderedItems.asSequence()
        .filterNot(selectedItems::contains)
        .take(CANDIDATE_LIMIT - selectedItems.size)
        .forEach(selectedItems::add)
    }
    return selectedItems
  }

  private data class Candidate(
    val item: SnapshotItem,
    val tier: CandidateTier,
    val changeBreadth: Int,
    val packageSizeDelta: Long
  )

  private enum class CandidateTier {
    TRACKED,
    NEW_INSTALL,
    VERSION_OR_ENVIRONMENT,
    STRUCTURAL,
    METADATA
  }

  private companion object {
    const val CANDIDATE_LIMIT = 12
    const val SYSTEM_APP_LIMIT = 2
    const val BASELINE_RECENT_APP_LIMIT = 3
    const val BASELINE_LARGE_APP_LIMIT = 2
    const val COMPARABLE_OVERLAP_RATIO = 0.6

    val candidateComparator = compareBy<Candidate>(Candidate::tier)
      .thenBy { it.item.isSystem }
      .thenByDescending(Candidate::changeBreadth)
      .thenByDescending(Candidate::packageSizeDelta)
      .thenByDescending { it.item.lastUpdatedTime }
      .thenBy { it.item.packageName }

    val landmarkComparator = compareBy<SnapshotItem>(SnapshotItem::isSystem)
      .thenByDescending(SnapshotItem::lastUpdatedTime)
      .thenByDescending(SnapshotItem::packageSize)
      .thenBy(SnapshotItem::packageName)
  }
}
