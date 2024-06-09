package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import android.text.SpannableString
import android.text.format.Formatter
import android.text.style.ImageSpan
import androidx.core.os.BundleCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.features.applist.detail.ui.view.OverlayDetailBottomSheetView
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersionString
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.getTargetApiString
import com.absinthe.libchecker.utils.extensions.getVersionString
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.runBlocking
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber

const val EXTRA_LC_ITEM = "EXTRA_LC_ITEM"

class OverlayDetailBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<OverlayDetailBottomSheetView>() {

  override fun initRootView(): OverlayDetailBottomSheetView =
    OverlayDetailBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val lcItem = arguments?.let {
      BundleCompat.getParcelable(it, EXTRA_LC_ITEM, LCItem::class.java)
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
            requireContext()
          )
          load(appIconLoader.loadIcon(packageInfo.applicationInfo))
          setOnLongClickListener {
            copyToClipboard()
            true
          }
        }
        appNameView.apply {
          text = packageInfo.getAppName()
          setLongClickCopiedToClipboard(text)
        }
        packageNameView.apply {
          text = lcItem.packageName
          setLongClickCopiedToClipboard(text)
        }
        versionInfoView.apply {
          text = packageInfo.getVersionString()
          setLongClickCopiedToClipboard(text)
        }
        extraInfoView.apply {
          text = buildSpannedString {
            append(Constants.OVERLAY_STRING).append(", ")
            scale(0.8f) {
              append("Target: ")
            }
            append(packageInfo.getTargetApiString())
            scale(0.8f) {
              append(" Min: ")
            }
            append(packageInfo.applicationInfo.minSdkVersion.toString())
            scale(0.8f) {
              append(" Compile: ")
            }
            append(packageInfo.getCompileSdkVersionString())
            scale(0.8f) {
              append(" Size: ")
            }
            val apkSize = FileUtils.getFileSize(packageInfo.applicationInfo.sourceDir)
            append(Formatter.formatFileSize(context, apkSize))
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
            .append(PackageUtils.getBuildVersionsInfo(pi, targetPackage))
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
            activity?.launchDetailPage(targetLCItem)
          }
        }
      }
    }
  }
}
