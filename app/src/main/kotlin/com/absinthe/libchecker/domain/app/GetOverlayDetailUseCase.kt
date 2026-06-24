package com.absinthe.libchecker.domain.app

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersionString
import com.absinthe.libchecker.utils.extensions.getTargetApiString
import com.absinthe.libchecker.utils.extensions.getVersionString
import com.absinthe.libchecker.utils.extensions.sizeToString
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetOverlayDetailUseCase(
  private val context: Context,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(lcItem: LCItem): Result = withContext(Dispatchers.IO) {
    val packageInfo = installedAppRepository.getPackageInfo(lcItem.packageName)
      ?: return@withContext Result.NotFound
    val applicationInfo = packageInfo.applicationInfo ?: return@withContext Result.NotFound
    Result.Available(
      OverlayDetailData(
        item = lcItem,
        packageInfo = packageInfo,
        packageName = lcItem.packageName,
        appName = packageInfo.getAppName(context.packageManager),
        versionInfo = packageInfo.getVersionString(),
        extraInfo = OverlayDetailExtraInfo(
          type = Constants.OVERLAY_STRING,
          targetSdkInfo = packageInfo.getTargetApiString(),
          minSdkInfo = applicationInfo.minSdkVersion.toString(),
          compileSdkInfo = packageInfo.getCompileSdkVersionString(),
          sizeInfo = FileUtils.getFileSize(applicationInfo.sourceDir)
            .sizeToString(context, showBytes = false)
        ),
        targetPackageName = Refine.unsafeCast<PackageInfoHidden>(packageInfo).overlayTarget
      )
    )
  }

  sealed interface Result {
    data class Available(val data: OverlayDetailData) : Result
    data object NotFound : Result
  }
}

data class OverlayDetailData(
  val item: LCItem,
  val packageInfo: PackageInfo,
  val packageName: String,
  val appName: String?,
  val versionInfo: String,
  val extraInfo: OverlayDetailExtraInfo,
  val targetPackageName: String?
)

data class OverlayDetailExtraInfo(
  val type: String,
  val targetSdkInfo: String,
  val minSdkInfo: String,
  val compileSdkInfo: String,
  val sizeInfo: String
)
