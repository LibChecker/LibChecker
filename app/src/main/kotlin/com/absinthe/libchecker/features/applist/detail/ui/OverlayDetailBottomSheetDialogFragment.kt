package com.absinthe.libchecker.features.applist.detail.ui

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
import com.absinthe.libchecker.domain.app.GetOverlayDetailUseCase
import com.absinthe.libchecker.domain.app.OverlayDetailData
import com.absinthe.libchecker.domain.app.OverlayDetailExtraInfo
import com.absinthe.libchecker.domain.app.RelatedAppDisplayData
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.features.applist.detail.ui.view.OverlayDetailBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_LC_ITEM = "EXTRA_LC_ITEM"

class OverlayDetailBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<OverlayDetailBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()

  override fun initRootView(): OverlayDetailBottomSheetView = OverlayDetailBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val lcItem = arguments?.let {
      BundleCompat.getParcelable(it, EXTRA_LC_ITEM, LCItem::class.java)
    } ?: return

    lifecycleScope.launch {
      when (val result = viewModel.getOverlayDetail(lcItem)) {
        GetOverlayDetailUseCase.Result.NotFound -> {
          Toasty.showShort(requireContext(), R.string.toast_cant_open_app)
          return@launch
        }

        is GetOverlayDetailUseCase.Result.Available -> {
          val data = result.data
          bindOverlayDetail(data)
          val targetPackage = data.targetPackageName ?: return@launch
          val target = viewModel.getRelatedAppListItem(targetPackage) ?: run {
            root.targetPackageView.addFloatView(targetPackage)
            return@launch
          }
          bindTargetPackageView(
            lcItem = data.item,
            data = viewModel.buildRelatedAppDisplayData(targetPackage, target)
          )
        }
      }
    }
  }

  private fun bindOverlayDetail(data: OverlayDetailData) {
    root.apply {
      detailsTitleView.apply {
        iconView.apply {
          val appIconLoader = AppIconLoader(
            resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
            false,
            requireContext()
          )
          data.packageInfo.applicationInfo?.let {
            load(appIconLoader.loadIcon(it))
          }
          setOnLongClickListener {
            copyToClipboard()
            true
          }
        }
        appNameView.apply {
          text = data.appName
          setLongClickCopiedToClipboard(text)
        }
        iconView.contentDescription = appNameView.text
        packageNameView.apply {
          text = data.packageName
          setLongClickCopiedToClipboard(text)
        }
        versionInfoView.apply {
          text = data.versionInfo
          setLongClickCopiedToClipboard(text)
        }
        extraInfoView.text = buildOverlayExtraInfo(data.extraInfo)
      }

      moreDetailButton.setOnClickListener {
        activity?.launchDetailPage(data.item, forceDetail = true)
      }
    }
  }

  private fun buildOverlayExtraInfo(extraInfo: OverlayDetailExtraInfo) = buildSpannedString {
    append(extraInfo.type).append(", ")
    scale(0.8f) {
      append("Target: ")
    }
    append(extraInfo.targetSdkInfo)
    scale(0.8f) {
      append(" Min: ")
    }
    append(extraInfo.minSdkInfo)
    scale(0.8f) {
      append(" Compile: ")
    }
    append(extraInfo.compileSdkInfo)
    scale(0.8f) {
      append(" Size: ")
    }
    append(extraInfo.sizeInfo)
  }

  private fun bindTargetPackageView(
    lcItem: LCItem,
    data: RelatedAppDisplayData
  ) {
    val context = requireContext()
    root.targetPackageView.container.let {
      it.icon.load(data.packageInfo)
      it.appName.text = data.label
      it.packageName.text = data.packageName
      it.versionInfo.text = data.versionInfo

      if (data.abiBadgeRes != null) {
        val spanString = SpannableString("  ${data.abiInfo}")
        data.abiBadgeRes.getDrawable(context)?.let { drawable ->
          drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
          val span = CenterAlignImageSpan(drawable)
          spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
        }
        it.abiInfo.text = spanString
      } else {
        it.abiInfo.text = data.abiInfo
      }
      root.targetPackageView.setItemContentDescription(
        root.targetTitleView.text,
        data.label,
        data.packageName,
        it.versionInfo.text,
        data.abiInfo
      )

      if (lcItem.variant == Constants.VARIANT_HAP) {
        it.setBadge(R.drawable.ic_harmony_badge)
      } else {
        it.setBadge(null)
      }

      root.targetPackageView.setOnClickListener {
        activity?.launchDetailPage(data.item)
      }
    }
  }
}
