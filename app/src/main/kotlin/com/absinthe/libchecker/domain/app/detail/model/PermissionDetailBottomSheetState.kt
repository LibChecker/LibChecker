package com.absinthe.libchecker.domain.app.detail.model

import android.graphics.drawable.Drawable

data class PermissionDetailContent(
  val name: String,
  val icon: Drawable?,
  val label: CharSequence?,
  val description: CharSequence?,
  val providerAppName: String?
)

sealed interface PermissionDetailBottomSheetState {
  val permissionName: String

  data class Loading(
    override val permissionName: String
  ) : PermissionDetailBottomSheetState

  data class Content(
    val detail: PermissionDetailContent
  ) : PermissionDetailBottomSheetState {
    override val permissionName: String = detail.name
  }
}
