package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.core.os.BundleCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.features.applist.detail.ui.view.OverlayDetailBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
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
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

const val EXTRA_LC_ITEM = "EXTRA_LC_ITEM"

class OverlayDetailBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<OverlayDetailBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()

  override fun initRootView(): OverlayDetailBottomSheetView = OverlayDetailBottomSheetView(requireContext())

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
          packageInfo.applicationInfo?.let {
            load(appIconLoader.loadIcon(it))
          }
          setOnLongClickListener {
            copyToClipboard()
            true
          }
        }
        appNameView.apply {
          text = packageInfo.getAppName(context.packageManager)
          setLongClickCopiedToClipboard(text)
        }
        iconView.contentDescription = appNameView.text
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
            append(packageInfo.applicationInfo?.minSdkVersion.toString())
            scale(0.8f) {
              append(" Compile: ")
            }
            append(packageInfo.getCompileSdkVersionString())
            scale(0.8f) {
              append(" Size: ")
            }
            val apkSize = FileUtils.getFileSize(packageInfo.applicationInfo!!.sourceDir)
            append(apkSize.sizeToString(context, showBytes = false))
          }
        }
      }

      targetPackageView.apply {
        container.let {
          val targetPackage = Refine.unsafeCast<PackageInfoHidden>(packageInfo).overlayTarget ?: return
          lifecycleScope.launch {
            val target = viewModel.getRelatedAppListItem(targetPackage) ?: run {
              addFloatView(targetPackage)
              return@launch
            }
            bindTargetPackageView(lcItem, targetPackage, target.item, target.packageInfo)
          }
        }
      }

      moreDetailButton.setOnClickListener {
        activity?.launchDetailPage(lcItem, forceDetail = true)
      }
    }
  }

  private fun bindTargetPackageView(
    lcItem: LCItem,
    targetPackage: String,
    targetLCItem: LCItem,
    pi: PackageInfo?
  ) {
    val context = requireContext()
    root.targetPackageView.container.let {
      it.icon.load(pi)
      it.appName.text = targetLCItem.label
      it.packageName.text = targetPackage
      it.versionInfo.text =
        PackageUtils.getVersionString(targetLCItem.versionName, targetLCItem.versionCode)

      val str = StringBuilder()
        .append(PackageUtils.getAbiString(context, targetLCItem.abi.toInt(), true))
        .append(PackageUtils.getBuildVersionsInfo(pi, targetPackage))
        .toString()
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
      root.targetPackageView.setItemContentDescription(
        root.targetTitleView.text,
        targetLCItem.label,
        targetPackage,
        it.versionInfo.text,
        str
      )

      if (lcItem.variant == Constants.VARIANT_HAP) {
        it.setBadge(R.drawable.ic_harmony_badge)
      } else {
        it.setBadge(null)
      }

      root.targetPackageView.setOnClickListener {
        activity?.launchDetailPage(targetLCItem)
      }
    }
  }
}
