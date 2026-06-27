package com.absinthe.libchecker.domain.app.list.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState

class GetAppListContentUseCase(
  private val ownPackageName: String,
  private val appListRepository: AppListRepository,
  private val appListSettingsRepository: AppListSettingsRepository,
  private val filterAppListItemsUseCase: FilterAppListItemsUseCase,
  private val buildAppListItemViewStatesUseCase: BuildAppListItemViewStatesUseCase
) {

  suspend operator fun invoke(request: Request): Result {
    val dbItems = appListRepository.getItems()
    if (isOnlySelfApp(dbItems)) {
      return Result.OnlySelf
    }
    val displayOptions = appListSettingsRepository.displayOptions

    val filteredItems = filterAppListItemsUseCase(
      FilterAppListItemsUseCase.Request(
        items = dbItems,
        options = displayOptions,
        keyword = request.keyword,
        isCurrentProcess64Bit = request.isCurrentProcess64Bit
      )
    )
    val itemViewStates = buildAppListItemViewStatesUseCase(
      BuildAppListItemViewStatesUseCase.Request(
        items = filteredItems,
        options = displayOptions
      )
    )

    return Result.Content(
      backingPackageNames = dbItems.mapTo(mutableSetOf()) { it.packageName },
      items = filteredItems,
      itemViewStates = itemViewStates
    )
  }

  suspend fun isOnlySelfAppInDatabase(): Boolean {
    return isOnlySelfApp(appListRepository.getItems())
  }

  private fun isOnlySelfApp(items: Collection<LCItem>): Boolean {
    return items.size == 1 && items.first().packageName == ownPackageName
  }

  data class Request(
    val keyword: String,
    val isCurrentProcess64Bit: Boolean
  )

  sealed interface Result {
    data object OnlySelf : Result

    data class Content(
      val backingPackageNames: Set<String>,
      val items: List<LCItem>,
      val itemViewStates: Map<String, AppListItemViewState>
    ) : Result
  }
}
