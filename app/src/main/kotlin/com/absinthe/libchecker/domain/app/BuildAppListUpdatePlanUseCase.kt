package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.database.entity.LCItem

class BuildAppListUpdatePlanUseCase {

  operator fun invoke(request: Request): Plan {
    return Plan(
      content = request.content,
      particleRemovalItemIds = buildParticleRemovalItemIds(
        currentItems = request.currentItems,
        backingPackageNames = request.content.backingPackageNames
      ),
      shouldClearPendingReturnTopAfterRequestChange = request.pendingReturnTopAfterRequestChange &&
        !request.highlightRefresh,
      shouldReturnTopAfterRequestChange = request.pendingReturnTopAfterRequestChange &&
        !request.highlightRefresh &&
        !request.hasUserScrolledList &&
        request.currentItems.isNotEmpty()
    )
  }

  private fun buildParticleRemovalItemIds(
    currentItems: List<LCItem>,
    backingPackageNames: Set<String>
  ): List<Long> {
    // Only apps that disappeared from the backing database get the particle effect.
    // Newly added apps go through RecyclerView's normal add path instead.
    return currentItems.asSequence()
      .filter { it.packageName !in backingPackageNames }
      .map { stableAppListItemIdForKey(it.packageName) }
      .toList()
  }

  data class Request(
    val currentItems: List<LCItem>,
    val content: GetAppListContentUseCase.Result.Content,
    val pendingReturnTopAfterRequestChange: Boolean,
    val highlightRefresh: Boolean,
    val hasUserScrolledList: Boolean
  )

  data class Plan(
    val content: GetAppListContentUseCase.Result.Content,
    val particleRemovalItemIds: List<Long>,
    val shouldClearPendingReturnTopAfterRequestChange: Boolean,
    val shouldReturnTopAfterRequestChange: Boolean
  )
}
