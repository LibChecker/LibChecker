package com.absinthe.libchecker.domain.app

import android.content.Context
import android.content.pm.PackageInfo
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildAppListItemViewStatesUseCase(
  private val context: Context,
  private val getAppListPackageStatesUseCase: GetAppListPackageStatesUseCase
) {

  suspend operator fun invoke(request: Request): Map<String, AppListItemViewState> = withContext(Dispatchers.IO) {
    val packageStates = getAppListPackageStatesUseCase(request.items)
    request.items.associate { item ->
      item.packageName to AppListItemViewState.create(
        context = context,
        item = item,
        packageState = packageStates.getValue(item.packageName),
        options = request.options
      )
    }
  }

  data class Request(
    val items: List<LCItem>,
    val options: Int
  )
}

data class AppListItemViewState(
  val packageInfo: PackageInfo?,
  val isPackageMissing: Boolean,
  val versionInfo: String,
  val abiInfo: CharSequence,
  val accessibilityAbiInfo: CharSequence,
  val useDetachedAbiBadges: Boolean,
  @DrawableRes val abiBadgeRes: Int,
  @DrawableRes val largeAbiBadgeRes: Int,
  val isAbiBadge64Bit: Boolean,
  val showMultiArchBadge: Boolean,
  val packageBadge: PackageBadge?
) {

  enum class PackageBadge {
    Harmony,
    Frozen
  }

  companion object {
    fun create(
      context: Context,
      item: LCItem,
      packageState: InstalledPackageState,
      options: Int
    ): AppListItemViewState {
      val abi = item.abi.toInt()
      val baseAbi = abi % Constants.MULTI_ARCH
      val buildVersionsInfo = PackageUtils.getBuildVersionsInfo(packageState.packageInfo, item.packageName)
      val abiInfo = StringBuilder()
        .append(PackageUtils.getAbiString(context, abi, false))
        .append(buildVersionsInfo)
      val accessibilityAbiInfo = StringBuilder()
        .append(PackageUtils.getAbiString(context, abi, true))
        .append(buildVersionsInfo)

      return AppListItemViewState(
        packageInfo = packageState.packageInfo,
        isPackageMissing = packageState.packageInfo == null && item.packageName != Constants.EXAMPLE_PACKAGE,
        versionInfo = PackageUtils.getVersionString(item.versionName, item.versionCode),
        abiInfo = abiInfo,
        accessibilityAbiInfo = accessibilityAbiInfo,
        useDetachedAbiBadges = shouldUseDetachedAbiBadges(options),
        abiBadgeRes = getAbiBadgeResource(abi),
        largeAbiBadgeRes = getLargeAbiBadgeResource(abi),
        isAbiBadge64Bit = PackageUtils.isAbi64Bit(baseAbi),
        showMultiArchBadge = abi / Constants.MULTI_ARCH == 1,
        packageBadge = when {
          item.packageName == Constants.EXAMPLE_PACKAGE -> null
          item.variant == Constants.VARIANT_HAP -> PackageBadge.Harmony
          packageState.isFrozen -> PackageBadge.Frozen
          else -> null
        }
      )
    }

    private fun shouldUseDetachedAbiBadges(options: Int): Boolean {
      return listOf(
        AdvancedOptions.SHOW_ANDROID_VERSION,
        AdvancedOptions.SHOW_TARGET_API,
        AdvancedOptions.SHOW_MIN_API,
        AdvancedOptions.SHOW_COMPILE_API
      ).count { (options and it) > 0 } >= 4
    }

    private fun getAbiBadgeResource(abi: Int): Int {
      return if (abi == Constants.OVERLAY || abi == Constants.ERROR) {
        0
      } else {
        PackageUtils.getAbiBadgeResource(abi)
      }
    }

    private fun getLargeAbiBadgeResource(abi: Int): Int {
      return if (abi == Constants.OVERLAY || abi == Constants.ERROR) {
        0
      } else {
        PackageUtils.getLargeAbiBadgeResource(abi)
      }
    }
  }
}
