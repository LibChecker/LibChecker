package com.absinthe.libchecker.domain.app.detail

import android.content.pm.PackageInfo
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.getStatefulPermissionsList

private const val LIVE_UPDATE_NOTIFICATION_PERMISSION = "android.permission.POST_PROMOTED_NOTIFICATIONS"

class GetAppDetailPermissionChipsUseCase {

  operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApk: Boolean,
    isApkPreview: Boolean
  ): AppDetailPermissionChips {
    val items = if (!isApkPreview && apkPreviewInfo == null) {
      packageInfo.getStatefulPermissionsList().asSequence()
        .map { permission ->
          val granted = permission.second && !isApk
          LibStringItemChip(
            LibStringItem(
              name = permission.first,
              size = if (granted) PackageInfo.REQUESTED_PERMISSION_GRANTED.toLong() else 0,
              source = if (permission.first.contains("maxSdkVersion")) DISABLED else null,
              process = if (granted) PackageInfo.REQUESTED_PERMISSION_GRANTED.toString() else null
            ),
            null
          )
        }
    } else {
      apkPreviewInfo!!.permissions.asSequence()
        .map { permission ->
          LibStringItemChip(
            LibStringItem(name = permission, size = 0, source = null, process = null),
            null
          )
        }
    }.toMutableList()

    items.sortByDescending { it.item.name }
    return AppDetailPermissionChips(
      items = items,
      hasLiveUpdateNotification = items.any { it.item.name == LIVE_UPDATE_NOTIFICATION_PERMISSION }
    )
  }
}

data class AppDetailPermissionChips(
  val items: List<LibStringItemChip>,
  val hasLiveUpdateNotification: Boolean
)
