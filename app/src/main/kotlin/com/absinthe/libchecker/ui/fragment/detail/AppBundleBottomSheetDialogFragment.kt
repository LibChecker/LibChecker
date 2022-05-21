package com.absinthe.libchecker.ui.fragment.detail

import com.absinthe.libchecker.bean.AppBundleItemBean
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.detail.AppBundleBottomSheetView
import com.absinthe.libchecker.view.detail.AppBundleItemView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.util.Locale

class AppBundleBottomSheetDialogFragment :
  BaseBottomSheetViewDialogFragment<AppBundleBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

  override fun initRootView(): AppBundleBottomSheetView =
    AppBundleBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    packageName?.let {
      val packageInfo = PackageUtils.getPackageInfo(it)
      val list = PackageUtils.getSplitsSourceDir(packageInfo)
      val localeList by lazy { Locale.getISOLanguages() }
      val bundleList = if (list.isNullOrEmpty()) {
        emptyList()
      } else {
        list.map { split ->
          val name = split.substringAfterLast("/")
          val middleName = name.removePrefix("split_config.").removeSuffix(".apk")
          val type = when {
            middleName.startsWith("arm") || middleName.startsWith("x86") -> AppBundleItemView.IconType.TYPE_NATIVE_LIBS
            middleName.endsWith("dpi") -> AppBundleItemView.IconType.TYPE_MATERIALS
            localeList.contains(middleName) -> AppBundleItemView.IconType.TYPE_STRINGS
            else -> AppBundleItemView.IconType.TYPE_OTHERS
          }
          AppBundleItemBean(name = name, size = FileUtils.getFileSize(split), type = type)
        }
      }
      root.adapter.setList(bundleList)
    }
  }
}
