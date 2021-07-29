package com.absinthe.libchecker.ui.fragment.detail

import com.absinthe.libchecker.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.bean.AppBundleItemBean
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.detail.AppBundleBottomSheetView
import com.absinthe.libchecker.view.detail.AppBundleItemView
import java.io.File
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
      val list = packageInfo.applicationInfo.splitSourceDirs
      val localeList by lazy { Locale.getISOLanguages() }
      val bundleList = if (list.isNullOrEmpty()) {
        emptyList()
      } else {
        list.map { split ->
          val name = split.substringAfterLast("/")
          val type = when {
            name.startsWith("split_config.arm") -> AppBundleItemView.IconType.TYPE_NATIVE_LIBS
            name.startsWith("split_config.x86") -> AppBundleItemView.IconType.TYPE_NATIVE_LIBS
            name.endsWith("dpi.apk") -> AppBundleItemView.IconType.TYPE_MATERIALS
            localeList.contains(
              name.removePrefix("split_config.").removeSuffix(".apk")
            ) -> AppBundleItemView.IconType.TYPE_STRINGS
            else -> AppBundleItemView.IconType.TYPE_OTHERS
          }
          AppBundleItemBean(name = name, size = File(split).length(), type = type)
        }
      }
      root.adapter.setList(bundleList)
    }
  }
}
