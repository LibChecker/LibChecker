package com.absinthe.libchecker.domain.app.detail.model

import android.content.Intent
import android.graphics.drawable.Drawable
import com.absinthe.libchecker.domain.app.detail.action.AppInfoLaunchAction
import com.absinthe.libchecker.domain.app.detail.action.AppInfoPrimaryActions

sealed interface AppInfoBottomSheetState {
  val packageName: String?

  data class Loading(
    override val packageName: String?
  ) : AppInfoBottomSheetState

  data class Content(
    override val packageName: String?,
    val primaryActions: AppInfoPrimaryActions,
    val externalActions: List<AppInfoActionItem>
  ) : AppInfoBottomSheetState
}

data class AppInfoActionItem(
  val packageName: String,
  val label: CharSequence,
  val icon: Drawable?,
  val intent: Intent
)

sealed interface AppInfoBottomSheetAction {
  data class Launch(
    val packageName: String?,
    val preloadedAction: AppInfoLaunchAction?
  ) : AppInfoBottomSheetAction

  data class OpenSettings(
    val packageName: String?,
    val preloadedIntent: Intent?
  ) : AppInfoBottomSheetAction

  data class Share(
    val packageName: String?
  ) : AppInfoBottomSheetAction

  data class Export(
    val packageName: String?
  ) : AppInfoBottomSheetAction

  data class OpenExternal(
    val item: AppInfoActionItem
  ) : AppInfoBottomSheetAction
}

val AppInfoBottomSheetState.launcherActivityCopyText: String?
  get() = (
    (this as? AppInfoBottomSheetState.Content)
      ?.primaryActions
      ?.launchAction as? AppInfoLaunchAction.Available
    )?.action?.launcherActivity

val AppInfoBottomSheetState.externalActions: List<AppInfoActionItem>
  get() = (this as? AppInfoBottomSheetState.Content)?.externalActions.orEmpty()

fun AppInfoBottomSheetState.launchAction(): AppInfoBottomSheetAction.Launch {
  return AppInfoBottomSheetAction.Launch(
    packageName = packageName,
    preloadedAction = (this as? AppInfoBottomSheetState.Content)?.primaryActions?.launchAction
  )
}

fun AppInfoBottomSheetState.settingsAction(): AppInfoBottomSheetAction.OpenSettings {
  return AppInfoBottomSheetAction.OpenSettings(
    packageName = packageName,
    preloadedIntent = (this as? AppInfoBottomSheetState.Content)?.primaryActions?.settingsIntent
  )
}
