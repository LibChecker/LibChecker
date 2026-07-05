package com.absinthe.libchecker.domain.app.detail

import android.content.Context
import android.content.pm.PackageInfo
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.related.RelatedAppListItem
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildRelatedAppDisplayDataUseCase(
  private val context: Context,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(
    packageName: String,
    relatedApp: RelatedAppListItem
  ): RelatedAppDisplayData {
    val isApexPackage = withContext(Dispatchers.IO) {
      packageName in installedAppRepository.getApexPackageNames()
    }
    val item = relatedApp.item
    val packageInfo = relatedApp.packageInfo
    val abi = item.abi.toInt()
    val abiBadgeRes = PackageUtils.getAbiBadgeResource(abi).takeIf {
      abi != Constants.OVERLAY && abi != Constants.ERROR && it != 0
    }
    return RelatedAppDisplayData(
      item = item,
      packageInfo = packageInfo,
      packageName = packageName,
      label = item.label,
      versionInfo = PackageUtils.getVersionString(item.versionName, item.versionCode),
      abiInfo = buildString {
        append(PackageUtils.getAbiString(context, abi, true))
        append(
          PackageUtils.getBuildVersionsInfo(
            packageInfo = packageInfo,
            packageName = packageName,
            isApexPackage = isApexPackage
          )
        )
      },
      abiBadgeRes = abiBadgeRes,
      isHarmony = item.variant == Constants.VARIANT_HAP
    )
  }
}

data class RelatedAppDisplayData(
  val item: LCItem,
  val packageInfo: PackageInfo?,
  val packageName: String,
  val label: String,
  val versionInfo: String,
  val abiInfo: String,
  @DrawableRes val abiBadgeRes: Int?,
  val isHarmony: Boolean
)
