package com.absinthe.libchecker.domain.app.detail.header

data class AppDetailHeaderExtraInfo(
  val targetSdkInfo: String,
  val minSdkInfo: String,
  val compileSdkInfo: String,
  val sizeInfo: String,
  val sharedUserId: String?
)
