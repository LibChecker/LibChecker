package com.absinthe.libchecker.domain.settings.model

sealed interface ExportAppsDialogState {
  data object Ready : ExportAppsDialogState
  data object Preparing : ExportAppsDialogState

  data class Exporting(
    val progress: Int
  ) : ExportAppsDialogState

  data object Done : ExportAppsDialogState
}

sealed interface ExportAppsDialogAction {
  data object PrimaryButtonClick : ExportAppsDialogAction
}
