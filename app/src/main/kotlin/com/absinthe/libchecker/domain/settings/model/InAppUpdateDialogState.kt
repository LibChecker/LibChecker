package com.absinthe.libchecker.domain.settings.model

import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData

data class InAppUpdateDialogState(
  val selectedChannel: AppUpdateChannel,
  val content: InAppUpdateDialogContent,
  val isChannelSelectionEnabled: Boolean,
  val isUpdateEnabled: Boolean
)

sealed interface InAppUpdateDialogContent {
  data class Loading(
    val retainedItem: SnapshotItemDisplayData? = null,
    val delayIndicator: Boolean = false
  ) : InAppUpdateDialogContent

  data class Ready(
    val item: SnapshotItemDisplayData?
  ) : InAppUpdateDialogContent
}

sealed interface InAppUpdateDialogAction {
  data class SelectChannel(
    val channel: AppUpdateChannel
  ) : InAppUpdateDialogAction

  data object Update : InAppUpdateDialogAction
}

fun InAppUpdateDialogState.selectChannel(channel: AppUpdateChannel): InAppUpdateDialogState {
  if (!isChannelSelectionEnabled || channel == selectedChannel) {
    return this
  }
  val retainedItem = when (val currentContent = content) {
    is InAppUpdateDialogContent.Loading -> currentContent.retainedItem
    is InAppUpdateDialogContent.Ready -> currentContent.item
  }
  return InAppUpdateDialogState(
    selectedChannel = channel,
    content = InAppUpdateDialogContent.Loading(retainedItem, delayIndicator = true),
    isChannelSelectionEnabled = false,
    isUpdateEnabled = false
  )
}

fun InAppUpdateDialogState.showContent(
  item: SnapshotItemDisplayData?,
  hasUpdate: Boolean
): InAppUpdateDialogState {
  return copy(
    content = InAppUpdateDialogContent.Ready(item),
    isChannelSelectionEnabled = true,
    isUpdateEnabled = hasUpdate
  )
}

fun InAppUpdateDialogState.showInstallProgress(): InAppUpdateDialogState {
  val retainedItem = when (val content = content) {
    is InAppUpdateDialogContent.Loading -> content.retainedItem
    is InAppUpdateDialogContent.Ready -> content.item
  }
  return copy(
    content = InAppUpdateDialogContent.Loading(retainedItem),
    isUpdateEnabled = false
  )
}
