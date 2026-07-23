package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.pm.PackageInfo
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.ui.MODE_SORT_BY_LIB
import com.absinthe.libchecker.domain.app.repository.AppDetailSettingsRepository

class DetailFilterController(
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
    return items.asSequence()
      .filterBySearchWords(searchWords)
      .filter { process == null || it.item.process == process }
      .toList()
  }

  fun filterPermissionDetailItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return items.asSequence()
      .filterBySearchWords(searchWords)
      .filter {
        process == null ||
          it.item.process != PackageInfo.REQUESTED_PERMISSION_GRANTED.toString()
      }
      .toList()
  }

  fun sortDetailItems(items: List<LibStringItemChip>, @LibType type: Int): List<LibStringItemChip> {
    return if (isSortByLibMode()) {
      if (type == NATIVE) {
        items.sortedByDescending { it.item.size }
      } else {
        items.sortedByDescending { it.item.name }
      }
    } else {
      items.sortedWith(compareByDescending<LibStringItemChip> { it.rule != null }.thenBy { it.item.name })
    }
  }

  fun buildProcessFilterData(
    @LibType type: Int,
    componentProcessesMap: Map<String, Int>,
    permissionItems: List<LibStringItemChip>?,
    permissionNotGrantedLabel: String,
    permissionNotGrantedColor: Int
  ): DetailProcessFilterData {
    val hasNonGrantedPermissions = hasNonGrantedPermissions(type, permissionItems)
    val processMap = when {
      isComponentDetailType(type) -> componentProcessesMap
      hasNonGrantedPermissions -> mapOf(permissionNotGrantedLabel to permissionNotGrantedColor)
      else -> emptyMap()
    }
    return DetailProcessFilterData(
      processMap = processMap,
      processToolIconVisible = processMap.isNotEmpty() && !hasNonGrantedPermissions
    )
  }

  fun isComponentDetailType(@LibType type: Int): Boolean {
    return isComponentType(type)
  }

  fun hasNonGrantedPermissions(@LibType type: Int, permissionItems: List<LibStringItemChip>?): Boolean {
    return type == PERMISSION && permissionItems?.any { it.item.size == 0L } == true
  }

  private fun isSortByLibMode(): Boolean {
    return appDetailSettingsRepository.sortMode == MODE_SORT_BY_LIB
  }

  private fun Sequence<LibStringItemChip>.filterBySearchWords(
    searchWords: String?
  ): Sequence<LibStringItemChip> {
    return filter {
      searchWords == null ||
        it.item.name.contains(searchWords, ignoreCase = true) ||
        it.item.source?.contains(searchWords, ignoreCase = true) == true
    }
  }
}

data class DetailProcessFilterData(
  val processMap: Map<String, Int>,
  val processToolIconVisible: Boolean
)
