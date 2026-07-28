package com.absinthe.libchecker.domain.app.detail.action

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.annotation.LibType

data class DetailItemLongClickActions(
  val packageName: String,
  val componentName: String,
  val fullComponentName: String,
  val copyText: String,
  val elfExtractAvailable: Boolean,
  val elfInfo: DetailItemElfInfoAction?,
  val reference: DetailItemReferenceAction?,
  val integrationsAvailable: Boolean,
  val providerPermissionAvailable: Boolean = false
)

data class DetailItemElfInfoAction(
  val packageName: String,
  val elfPath: String,
  @DrawableRes val ruleIcon: Int
)

data class DetailItemReferenceAction(
  val refName: String,
  val label: String?,
  @LibType val type: Int
)
