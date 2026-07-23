package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.annotation.LibType

sealed interface DetailItemDialogRequest {
  data class Permission(val permissionName: String) : DetailItemDialogRequest

  data class Library(
    val name: String,
    @LibType val type: Int,
    val regexName: String?,
    val isValidLib: Boolean
  ) : DetailItemDialogRequest
}
