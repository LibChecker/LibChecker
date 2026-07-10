package com.absinthe.libchecker.domain.settings.model

import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo

sealed interface CloudRulesDialogState {
  data object Loading : CloudRulesDialogState

  data class Content(
    val localVersion: Int,
    val remoteVersion: Int,
    val updateAvailable: Boolean
  ) : CloudRulesDialogState
}

sealed interface CloudRulesDialogAction {
  data object Update : CloudRulesDialogAction
}

fun CloudRulesVersionInfo.toCloudRulesDialogState(): CloudRulesDialogState.Content {
  return CloudRulesDialogState.Content(
    localVersion = localVersion,
    remoteVersion = remoteVersion,
    updateAvailable = updateAvailable
  )
}
