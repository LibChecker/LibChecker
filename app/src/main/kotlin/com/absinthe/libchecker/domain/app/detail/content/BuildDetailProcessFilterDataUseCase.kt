package com.absinthe.libchecker.domain.app.detail.content

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip

class BuildDetailProcessFilterDataUseCase {

  operator fun invoke(
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

  fun hasNonGrantedPermissions(
    @LibType type: Int,
    permissionItems: List<LibStringItemChip>?
  ): Boolean {
    return type == PERMISSION && permissionItems?.any { it.item.size == 0L } == true
  }
}

data class DetailProcessFilterData(
  val processMap: Map<String, Int>,
  val processToolIconVisible: Boolean
)
