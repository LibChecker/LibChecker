package com.absinthe.libchecker.ui.fragment.detail

import android.annotation.SuppressLint
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import android.text.SpannableString
import android.text.style.ImageSpan
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.SystemServices
import com.absinthe.libchecker.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.view.detail.OverlayDetailBottomSheetView
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

  @SuppressLint("SetTextI18n")
  override fun init() {
    val lcItem = (arguments?.getParcelable(EXTRA_LC_ITEM) as? LCItem) ?: return
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
          text = "${Constants.OVERLAY_STRING}, ${PackageUtils.getTargetApiString(packageInfo)}"
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

          try {
            val ai = PackageUtils.getPackageInfo(targetPackage).applicationInfo
            it.icon.load(ai.loadIcon(SystemServices.packageManager))
          } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e)
          }

          it.appName.text = targetLCItem.label
          it.packageName.text = targetPackage
          it.versionInfo.text =
            PackageUtils.getVersionString(targetLCItem.versionName, targetLCItem.versionCode)

          val str = StringBuilder()
            .append(PackageUtils.getAbiString(context, targetLCItem.abi.toInt(), true))
            .append(", ")
            .append(PackageUtils.getTargetApiString(targetLCItem.targetApi))
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
}
