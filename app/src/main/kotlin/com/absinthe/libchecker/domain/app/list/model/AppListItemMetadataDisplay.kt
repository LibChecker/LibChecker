package com.absinthe.libchecker.domain.app.list.model

import androidx.annotation.DrawableRes

data class AppListItemMetadataDisplay(
  val versionInfo: String,
  val abiInfo: CharSequence,
  val useDetachedAbiBadges: Boolean,
  @DrawableRes val abiBadgeRes: Int,
  @DrawableRes val largeAbiBadgeRes: Int,
  val isAbiBadge64Bit: Boolean,
  val showMultiArchBadge: Boolean,
  val tintAbiLabels: Boolean,
  val packageBadge: PackageBadge?
) {

  enum class PackageBadge {
    Harmony,
    Frozen
  }

  companion object {
    fun create(viewState: AppListItemViewState): AppListItemMetadataDisplay {
      return AppListItemMetadataDisplay(
        versionInfo = viewState.versionInfo,
        abiInfo = viewState.abiInfo,
        useDetachedAbiBadges = viewState.useDetachedAbiBadges,
        abiBadgeRes = viewState.abiBadgeRes,
        largeAbiBadgeRes = viewState.largeAbiBadgeRes,
        isAbiBadge64Bit = viewState.isAbiBadge64Bit,
        showMultiArchBadge = viewState.showMultiArchBadge,
        tintAbiLabels = viewState.tintAbiLabels,
        packageBadge = when (viewState.packageBadge) {
          AppListItemViewState.PackageBadge.Harmony -> PackageBadge.Harmony
          AppListItemViewState.PackageBadge.Frozen -> PackageBadge.Frozen
          null -> null
        }
      )
    }
  }
}
