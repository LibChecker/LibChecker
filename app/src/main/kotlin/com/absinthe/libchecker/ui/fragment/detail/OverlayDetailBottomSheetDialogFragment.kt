package com.absinthe.libchecker.ui.fragment.detail

import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import android.os.Build
import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.SystemServices
import com.absinthe.libchecker.compat.BundleCompat
import com.absinthe.libchecker.constant.AdvancedOptions
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.view.detail.OverlayDetailBottomSheetView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.runBlocking
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber

const val EXTRA_LC_ITEM = "EXTRA_LC_ITEM"

class OverlayDetailBottomSheetDialogFragment :
  BaseBottomSheetViewDialogFragment<OverlayDetailBottomSheetView>() {

  override fun initRootView(): OverlayDetailBottomSheetView =
    OverlayDetailBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val lcItem = arguments?.let {
      BundleCompat.getParcelable<LCItem>(it, EXTRA_LC_ITEM)
    } ?: return
    val packageInfo = try {
      PackageUtils.getPackageInfo(lcItem.packageName)
    } catch (e: PackageManager.NameNotFoundException) {
      Timber.e(e)
      Toasty.showShort(requireContext(), e.toString())
      return
    }

    root.apply {
      detailsTitleView.apply {
        iconView.apply {
          val appIconLoader = AppIconLoader(
            resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
            false,
            requireContext(),
          )
          load(appIconLoader.loadIcon(packageInfo.applicationInfo))
        }
        appNameView.apply {
          text = packageInfo.applicationInfo.loadLabel(SystemServices.packageManager).toString()
          setLongClickCopiedToClipboard(text)
        }
        packageNameView.apply {
          text = lcItem.packageName
          setLongClickCopiedToClipboard(text)
        }
        versionInfoView.apply {
          text = PackageUtils.getVersionString(packageInfo)
          setLongClickCopiedToClipboard(text)
        }
        extraInfoView.apply {
          text = buildSpannedString {
            append(Constants.OVERLAY_STRING).append(", ")
            scale(0.8f) {
              append("Target: ")
            }
            append(PackageUtils.getTargetApiString(packageInfo))
            scale(0.8f) {
              append(" Min: ")
            }
            append(PackageUtils.getMinSdkVersion(packageInfo).toString())
          }
        }
      }

      targetPackageView.apply {
        container.let {
          val targetPackage = Refine.unsafeCast<PackageInfoHidden>(packageInfo).overlayTarget
          val targetLCItem = runBlocking { Repositories.lcRepository.getItem(targetPackage) }

          if (targetLCItem == null) {
            addFloatView(targetPackage)
            return
          }

          val pi = runCatching {
            PackageUtils.getPackageInfo(targetPackage)
          }.getOrNull()
          it.icon.load(pi)
          it.appName.text = targetLCItem.label
          it.packageName.text = targetPackage
          it.versionInfo.text =
            PackageUtils.getVersionString(targetLCItem.versionName, targetLCItem.versionCode)

          val str = StringBuilder()
            .append(PackageUtils.getAbiString(context, targetLCItem.abi.toInt(), true))
            .append(getBuildVersionsInfo(pi, targetPackage))
          val spanString: SpannableString
          val abiBadgeRes = PackageUtils.getAbiBadgeResource(targetLCItem.abi.toInt())

          if (targetLCItem.abi.toInt() != Constants.OVERLAY && targetLCItem.abi.toInt() != Constants.ERROR && abiBadgeRes != 0) {
            spanString = SpannableString("  $str")
            abiBadgeRes.getDrawable(context)?.let { drawable ->
              drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
              val span = CenterAlignImageSpan(drawable)
              spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
            }
            it.abiInfo.text = spanString
          } else {
            it.abiInfo.text = str
          }

          if (lcItem.variant == Constants.VARIANT_HAP) {
            it.setBadge(R.drawable.ic_harmony_badge)
          } else {
            it.setBadge(null)
          }

          targetPackageView.setOnClickListener {
            LCAppUtils.launchDetailPage(requireActivity(), targetLCItem)
          }
        }
      }
    }
  }

  private fun getBuildVersionsInfo(packageInfo: PackageInfo?, packageName: String): CharSequence {
    if (packageInfo == null && packageName != Constants.EXAMPLE_PACKAGE) {
      return ""
    }
    val showAndroidVersion =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_ANDROID_VERSION) > 0
    val showTarget =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_TARGET_API) > 0
    val showMin =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_MIN_API) > 0
    val target = packageInfo?.applicationInfo?.targetSdkVersion ?: Build.VERSION.SDK_INT
    val min = packageInfo?.applicationInfo?.minSdkVersion ?: Build.VERSION.SDK_INT

    return buildSpannedString {
      if (showTarget) {
        append(", ")
        scale(0.8f) {
          append("Target: ")
        }
        append(target.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[target]})")
        }
      }

      if (showMin) {
        if (showTarget) {
          append(", ")
        }
        scale(0.8f) {
          append(" Min: ")
        }
        append(min.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[min]})")
        }
      }
    }
  }
}
