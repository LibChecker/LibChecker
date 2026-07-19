package com.absinthe.libchecker.domain.app.list.model

data class AppListItemDisplay(
  val icon: AppListItemIconDisplay,
  val identity: AppListItemIdentityText,
  val metadata: AppListItemMetadataDisplay,
  val chips: List<String> = emptyList()
) {

  companion object {
    fun create(
      label: String,
      packageName: String,
      viewState: AppListItemViewState,
      showMissingPackageStrikeThrough: Boolean,
      chips: List<String> = emptyList()
    ): AppListItemDisplay {
      return AppListItemDisplay(
        icon = AppListItemIconDisplay.create(packageName, viewState.packageInfo),
        identity = AppListItemIdentityText.create(
          label = label,
          packageName = packageName,
          versionInfo = viewState.versionInfo,
          accessibilityAbiInfo = viewState.accessibilityAbiInfo,
          showMissingPackageStrikeThrough = showMissingPackageStrikeThrough
        ),
        metadata = AppListItemMetadataDisplay.create(viewState),
        chips = chips
      )
    }
  }
}
