package com.absinthe.libchecker.domain.app.detail.model

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.action.AppInstalledTimeDisplayData
import com.absinthe.libchecker.domain.app.detail.related.RelatedAppDisplayData
import com.absinthe.libchecker.utils.extensions.DexFileOptimizationInfo

data class AppInstallSourceBottomSheetDisplay(
  val originatingApp: AppInstallSourceItemDisplay?,
  val installingApp: AppInstallSourceItemDisplay?,
  val installedTime: AppInstalledTimeDisplayData?,
  val dexoptInfo: DexFileOptimizationInfo?
)

data class AppInstallSourceItemDisplay(
  val title: String,
  val content: AppInstallSourceItemContent,
  val contentDescription: String,
  val action: AppInstallSourceAction?
)

sealed interface AppInstallSourceItemContent {
  data class RelatedApp(
    val data: RelatedAppDisplayData
  ) : AppInstallSourceItemContent

  data class Message(
    @DrawableRes val iconRes: Int,
    val appName: String,
    val packageName: String,
    val versionInfo: String,
    val abiInfo: String,
    val showAbiInfo: Boolean
  ) : AppInstallSourceItemContent
}

enum class AppInstallSourceRequesterAccess {
  Available,
  ShizukuNotRunning,
  ShizukuLowVersion,
  ShizukuPermissionDenied
}

sealed interface AppInstallSourceAction {
  data class OpenApp(
    val item: LCItem
  ) : AppInstallSourceAction

  data object RequestShizukuPermission : AppInstallSourceAction
}
