package com.absinthe.libchecker.domain.app.detail.action

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.RelatedAppDisplayData
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceAction
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemContent
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceRequesterAccess
import com.absinthe.libchecker.domain.app.list.model.buildAppListItemDescription

class BuildAppInstallSourceBottomSheetDisplayUseCase(
  private val context: Context
) {

  operator fun invoke(request: Request): AppInstallSourceBottomSheetDisplay {
    return buildAppInstallSourceBottomSheetDisplay(request, context.displayStrings())
  }

  data class Request(
    val details: AppInstallSourceDetails,
    val originatingApp: RelatedAppDisplayData?,
    val installingApp: RelatedAppDisplayData?,
    val requesterAccess: AppInstallSourceRequesterAccess
  )

  private fun Context.displayStrings(): AppInstallSourceDisplayStrings {
    return AppInstallSourceDisplayStrings(
      originatingTitle = getString(R.string.lib_detail_app_install_source_originating_package),
      installingTitle = getString(R.string.lib_detail_app_install_source_installing_package),
      unknownAppName = getString(R.string.lib_detail_app_install_source_empty),
      unknownPackageDescription = getString(R.string.lib_detail_app_install_source_empty_detail),
      shizukuUsage = getString(R.string.lib_detail_app_install_source_shizuku_usage),
      shizukuPrompts = mapOf(
        AppInstallSourceRequesterAccess.ShizukuNotInstalled to ShizukuPromptStrings(
          appName = getString(R.string.lib_detail_app_install_source_shizuku_uninstalled),
          detail = getString(R.string.lib_detail_app_install_source_shizuku_uninstalled_detail)
        ),
        AppInstallSourceRequesterAccess.ShizukuNotRunning to ShizukuPromptStrings(
          appName = getString(R.string.lib_detail_app_install_source_shizuku_not_running),
          detail = getString(R.string.lib_detail_app_install_source_shizuku_not_running_detail)
        ),
        AppInstallSourceRequesterAccess.ShizukuLowVersion to ShizukuPromptStrings(
          appName = getString(R.string.lib_detail_app_install_source_shizuku_low_version),
          detail = getString(R.string.lib_detail_app_install_source_shizuku_low_version_detail)
        ),
        AppInstallSourceRequesterAccess.ShizukuPermissionDenied to ShizukuPromptStrings(
          appName = getString(R.string.lib_detail_app_install_source_shizuku_permission_not_granted),
          detail = getString(R.string.lib_detail_app_install_source_shizuku_permission_not_granted_detail)
        )
      )
    )
  }
}

internal data class AppInstallSourceDisplayStrings(
  val originatingTitle: String,
  val installingTitle: String,
  val unknownAppName: String,
  val unknownPackageDescription: String,
  val shizukuUsage: String,
  val shizukuPrompts: Map<AppInstallSourceRequesterAccess, ShizukuPromptStrings>
)

internal data class ShizukuPromptStrings(
  val appName: String,
  val detail: String
)

internal fun buildAppInstallSourceBottomSheetDisplay(
  request: BuildAppInstallSourceBottomSheetDisplayUseCase.Request,
  strings: AppInstallSourceDisplayStrings
): AppInstallSourceBottomSheetDisplay {
  val installSource = request.details.installSource
  val originatingApp = installSource?.let {
    if (request.requesterAccess == AppInstallSourceRequesterAccess.Available) {
      buildPackageItemDisplay(
        title = strings.originatingTitle,
        packageName = it.originatingPackageName,
        data = request.originatingApp,
        strings = strings
      )
    } else {
      buildShizukuPromptDisplay(
        title = strings.originatingTitle,
        requesterAccess = request.requesterAccess,
        strings = strings
      )
    }
  }
  val installingApp = installSource?.let {
    buildPackageItemDisplay(
      title = strings.installingTitle,
      packageName = it.installingPackageName,
      data = request.installingApp,
      strings = strings
    )
  }
  return AppInstallSourceBottomSheetDisplay(
    originatingApp = originatingApp,
    installingApp = installingApp,
    installedTime = request.details.installedTime,
    dexoptInfo = request.details.dexoptInfo
  )
}

private fun buildPackageItemDisplay(
  title: String,
  packageName: String?,
  data: RelatedAppDisplayData?,
  strings: AppInstallSourceDisplayStrings
): AppInstallSourceItemDisplay? {
  if (packageName == null) {
    return AppInstallSourceItemDisplay(
      title = title,
      content = AppInstallSourceItemContent.Message(
        iconRes = R.drawable.ic_icon_blueprint,
        appName = strings.unknownAppName,
        packageName = strings.unknownPackageDescription,
        versionInfo = VERSION_PLACEHOLDER,
        abiInfo = ABI_PLACEHOLDER,
        showAbiInfo = true
      ),
      contentDescription = buildAppListItemDescription(
        title,
        strings.unknownAppName,
        strings.unknownPackageDescription
      ),
      action = null
    )
  }
  data ?: return null
  return AppInstallSourceItemDisplay(
    title = title,
    content = AppInstallSourceItemContent.RelatedApp(data),
    contentDescription = buildAppListItemDescription(
      title,
      data.label,
      data.packageName,
      data.versionInfo,
      data.abiInfo
    ),
    action = AppInstallSourceAction.OpenApp(data.item)
  )
}

private fun buildShizukuPromptDisplay(
  title: String,
  requesterAccess: AppInstallSourceRequesterAccess,
  strings: AppInstallSourceDisplayStrings
): AppInstallSourceItemDisplay {
  val prompt = checkNotNull(strings.shizukuPrompts[requesterAccess]) {
    "Shizuku prompt strings are required for $requesterAccess."
  }
  val action = when (requesterAccess) {
    AppInstallSourceRequesterAccess.ShizukuNotInstalled,
    AppInstallSourceRequesterAccess.ShizukuLowVersion -> {
      AppInstallSourceAction.OpenShizukuReleasePage
    }

    AppInstallSourceRequesterAccess.ShizukuNotRunning -> {
      AppInstallSourceAction.LaunchShizuku
    }

    AppInstallSourceRequesterAccess.ShizukuPermissionDenied -> {
      AppInstallSourceAction.RequestShizukuPermission
    }

    AppInstallSourceRequesterAccess.Available -> {
      error("Available requester access does not require a Shizuku prompt.")
    }
  }
  return AppInstallSourceItemDisplay(
    title = title,
    content = AppInstallSourceItemContent.Message(
      iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_shizuku,
      appName = prompt.appName,
      packageName = strings.shizukuUsage,
      versionInfo = prompt.detail,
      abiInfo = "",
      showAbiInfo = false
    ),
    contentDescription = buildAppListItemDescription(
      title,
      prompt.appName,
      strings.shizukuUsage,
      prompt.detail
    ),
    action = action
  )
}

private const val VERSION_PLACEHOLDER =
  "                                                                            "
private const val ABI_PLACEHOLDER = "                                            "
