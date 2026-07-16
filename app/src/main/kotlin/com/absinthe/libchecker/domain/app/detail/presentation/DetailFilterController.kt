package com.absinthe.libchecker.domain.app.detail.presentation

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.domain.app.detail.content.BuildDetailProcessFilterDataUseCase
import com.absinthe.libchecker.domain.app.detail.content.FilterAppDetailItemsUseCase
import com.absinthe.libchecker.domain.app.detail.content.SortAppDetailItemsUseCase
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.ui.MODE_SORT_BY_LIB
import com.absinthe.libchecker.domain.app.repository.AppDetailSettingsRepository

class DetailFilterController(
  private val filterAppDetailItemsUseCase: FilterAppDetailItemsUseCase,
  private val sortAppDetailItemsUseCase: SortAppDetailItemsUseCase,
  private val buildDetailProcessFilterDataUseCase: BuildDetailProcessFilterDataUseCase,
  private val appDetailSettingsRepository: AppDetailSettingsRepository
) {
  val filterState = DetailFilterState()

  fun reset() {
    filterState.reset()
  }

  fun filterDetailItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return filterAppDetailItemsUseCase(items, searchWords, process)
  }

  fun filterPermissionDetailItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return filterAppDetailItemsUseCase.filterPermissions(items, searchWords, process)
  }

  fun sortDetailItems(items: List<LibStringItemChip>, @LibType type: Int): List<LibStringItemChip> {
    return sortAppDetailItemsUseCase(items, type, isSortByLibMode())
  }

  fun buildProcessFilterData(
    @LibType type: Int,
    componentProcessesMap: Map<String, Int>,
    permissionItems: List<LibStringItemChip>?,
    permissionNotGrantedLabel: String,
    permissionNotGrantedColor: Int
  ) = buildDetailProcessFilterDataUseCase(
    type = type,
    componentProcessesMap = componentProcessesMap,
    permissionItems = permissionItems,
    permissionNotGrantedLabel = permissionNotGrantedLabel,
    permissionNotGrantedColor = permissionNotGrantedColor
  )

  fun isComponentDetailType(@LibType type: Int): Boolean {
    return buildDetailProcessFilterDataUseCase.isComponentDetailType(type)
  }

  fun hasNonGrantedPermissions(@LibType type: Int, permissionItems: List<LibStringItemChip>?): Boolean {
    return buildDetailProcessFilterDataUseCase.hasNonGrantedPermissions(type, permissionItems)
  }

  private fun isSortByLibMode(): Boolean {
    return appDetailSettingsRepository.sortMode == MODE_SORT_BY_LIB
  }
}
