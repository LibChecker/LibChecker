package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import androidx.core.os.BundleCompat
import com.absinthe.libchecker.features.applist.detail.bean.AppBundleItem
import com.absinthe.libchecker.features.applist.detail.ui.view.AppBundleBottomSheetView
import com.absinthe.libchecker.features.applist.detail.ui.view.AppBundleItemView
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.STRING_ABI_MAP
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.io.File
import java.util.Locale

class AppBundleBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppBundleBottomSheetView>() {

  private val packageInfo by lazy { BundleCompat.getParcelable(requireArguments(), EXTRA_PACKAGE_INFO, PackageInfo::class.java) }

  override fun initRootView(): AppBundleBottomSheetView = AppBundleBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    packageInfo?.let {
      val list = PackageUtils.getSplitsSourceDir(it)
      val localeList by lazy { Locale.getISOLanguages() }
      val bundleList = if (list.isNullOrEmpty()) {
        emptyList()
      } else {
        list.map { split ->
          val name = split.substringAfterLast(File.separator)
          val middleName = name.removeSurrounding("split_config.", ".apk")
          val type = when {
            STRING_ABI_MAP.keys.any { arch -> middleName.contains(arch) } -> AppBundleItemView.IconType.TYPE_NATIVE_LIBS
            middleName.endsWith("dpi") -> AppBundleItemView.IconType.TYPE_MATERIALS
            localeList.contains(middleName) -> AppBundleItemView.IconType.TYPE_STRINGS
            else -> AppBundleItemView.IconType.TYPE_OTHERS
          }
          AppBundleItem(name = name, size = FileUtils.getFileSize(split), type = type)
        }
      }
      root.adapter.setList(bundleList)
    }
  }
}
