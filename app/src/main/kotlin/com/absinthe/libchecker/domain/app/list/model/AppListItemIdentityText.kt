package com.absinthe.libchecker.domain.app.list.model

data class AppListItemIdentityText(
  val label: String,
  val packageName: String,
  val contentDescription: String,
  val showMissingPackageStrikeThrough: Boolean
) {

  companion object {
    fun create(
      label: String,
      packageName: String,
      versionInfo: CharSequence?,
      accessibilityAbiInfo: CharSequence?,
      showMissingPackageStrikeThrough: Boolean
    ): AppListItemIdentityText {
      return AppListItemIdentityText(
        label = label,
        packageName = packageName,
        contentDescription = buildAppListItemDescription(
          label,
          packageName,
          versionInfo,
          accessibilityAbiInfo
        ),
        showMissingPackageStrikeThrough = showMissingPackageStrikeThrough
      )
    }
  }
}
