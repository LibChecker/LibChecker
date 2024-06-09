package com.absinthe.libchecker.features.applist.detail.ui

import com.absinthe.libchecker.features.applist.detail.bean.AppBundleItem
import com.absinthe.libchecker.features.applist.detail.ui.view.AppBundleBottomSheetView
import com.absinthe.libchecker.features.applist.detail.ui.view.AppBundleItemView
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.util.Locale

class AppBundleBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppBundleBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

  override fun initRootView(): AppBundleBottomSheetView =
    AppBundleBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    packageName?.let {
      val packageInfo = PackageUtils.getPackageInfo(it)
      val list = PackageUtils.getSplitsSourceDir(packageInfo)
      val localeList by lazy { Locale.getISOLanguages() }
      val bundleList = if (list.isNullOrEmpty()) {
        emptyList()
      } else {
        list.map { split ->
          val name = split.substringAfterLast("/")
          val middleName = name.removeSurrounding("split_config.", ".apk")
          val type = when {
            middleName.startsWith("arm") || middleName.startsWith("x86") -> AppBundleItemView.IconType.TYPE_NATIVE_LIBS
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
