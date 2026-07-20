package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotListRenderState
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildSnapshotListUpdatePlanUseCase(
  private val getSnapshotPackageIconSources: GetSnapshotPackageIconSourcesUseCase,
  private val getApexPackageNames: GetApexPackageNamesUseCase,
  private val snapshotSettingsRepository: SnapshotSettingsRepository
) {

  suspend operator fun invoke(request: Request): Plan {
    val stableRequest = request.freezeCollections()
    val displayPlan = withContext(Dispatchers.Default) {
      buildDisplayPlan(stableRequest)
    }
    val packageNames = displayPlan.items.map(SnapshotDiffItem::packageName)

    return displayPlan.copy(
      renderState = displayPlan.renderState.copy(
        packageIconSources = getSnapshotPackageIconSources(packageNames),
        apexPackageNames = getApexPackageNames()
      )
    )
  }

  private fun buildDisplayPlan(request: Request): Plan {
    val hideNoComponentChanges =
      snapshotSettingsRepository.options.and(SnapshotOptions.HIDE_NO_COMPONENT_CHANGES) != 0
    val displayOptions = snapshotSettingsRepository.listDisplayOptions
    val sortedItems = request.sourceItems.asSequence()
      .filter { it.matchesSearchKeyword(request.searchKeyword) }
      .filterNot { hideNoComponentChanges && it.isNothingChanged() }
      .sortedByDescending { it.updateTime }
      .toList()

    if (request.highlightRefresh) {
      return Plan(
        items = sortedItems,
        renderState = SnapshotListRenderState(
          displayOptions = displayOptions,
          highlightText = request.searchKeyword
        ),
        particleRemovalItemIds = emptyList(),
        consumedRemovePackageNames = emptySet()
      )
    }

    val newPackageNames = sortedItems.mapTo(mutableSetOf()) { it.packageName }
    val pendingRemovePackageNames = request.pendingRemovePackageNames
    val deletedReplacementPackageNames = sortedItems.asSequence()
      .filter { it.deleted && it.packageName in pendingRemovePackageNames }
      .mapTo(mutableSetOf()) { it.packageName }
    val consumedRemovePackageNames = mutableSetOf<String>()
    val particleRemovalItemIds = request.currentItems.asSequence()
      .filter {
        val shouldAnimate = it.packageName !in newPackageNames ||
          it.packageName in deletedReplacementPackageNames
        if (shouldAnimate) {
          consumedRemovePackageNames += it.packageName
        }
        shouldAnimate
      }
      .map(::stableSnapshotDiffItemIdFor)
      .toList()

    return Plan(
      items = sortedItems,
      renderState = SnapshotListRenderState(
        displayOptions = displayOptions,
        highlightText = request.searchKeyword
      ),
      particleRemovalItemIds = particleRemovalItemIds,
      consumedRemovePackageNames = consumedRemovePackageNames
    )
  }

  private fun SnapshotDiffItem.matchesSearchKeyword(keyword: String): Boolean {
    if (keyword.isEmpty()) {
      return true
    }
    return packageName.contains(keyword, ignoreCase = true) ||
      labelDiff.old.contains(keyword, ignoreCase = true) ||
      labelDiff.new?.contains(keyword, ignoreCase = true) == true
  }

  data class Request(
    val currentItems: List<SnapshotDiffItem>,
    val sourceItems: List<SnapshotDiffItem>,
    val searchKeyword: String,
    val pendingRemovePackageNames: Set<String>,
    val highlightRefresh: Boolean
  ) {
    internal fun freezeCollections(): Request {
      return copy(
        currentItems = currentItems.toList(),
        sourceItems = sourceItems.toList(),
        pendingRemovePackageNames = pendingRemovePackageNames.toSet()
      )
    }
  }

  data class Plan(
    val items: List<SnapshotDiffItem>,
    val renderState: SnapshotListRenderState,
    val particleRemovalItemIds: List<Long>,
    val consumedRemovePackageNames: Set<String>
  )
}

fun stableSnapshotDiffItemIdFor(item: SnapshotDiffItem): Long {
  val state = when {
    item.deleted -> "deleted"
    item.newInstalled -> "new"
    else -> "normal"
  }
  return "${item.packageName}:$state".hashCode().toLong()
}
