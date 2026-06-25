package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip

class BuildDetailItemDialogRequestUseCase {

  operator fun invoke(
    item: LibStringItemChip,
    @LibType detailType: Int
  ): DetailItemDialogRequest {
    if (detailType == PERMISSION) {
      return DetailItemDialogRequest.Permission(item.item.name)
    }

    val rule = item.rule
    return DetailItemDialogRequest.Library(
      name = rule?.libName ?: item.item.name,
      type = if (rule?.libType == ACTION_IN_RULES) ACTION else detailType,
      regexName = rule?.regexName,
      isValidLib = rule != null
    )
  }
}

sealed interface DetailItemDialogRequest {
  data class Permission(val permissionName: String) : DetailItemDialogRequest

  data class Library(
    val name: String,
    @LibType val type: Int,
    val regexName: String?,
    val isValidLib: Boolean
  ) : DetailItemDialogRequest
}
