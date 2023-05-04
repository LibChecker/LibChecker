package com.absinthe.libchecker.ui.fragment.detail

import com.absinthe.libchecker.model.AppPropItem
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.manifest.ApplicationReader
import com.absinthe.libchecker.view.detail.AppPropsBottomSheetView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.io.File

class AppPropBottomSheetDialogFragment :
  BaseBottomSheetViewDialogFragment<AppPropsBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

  override fun initRootView(): AppPropsBottomSheetView =
    AppPropsBottomSheetView(requireContext(), PackageUtils.getPackageInfo(packageName!!))

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.post {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.67).toInt()
    }
    packageName?.let {
      val packageInfo = PackageUtils.getPackageInfo(it)
      val propsMap =
        ApplicationReader.getManifestProperties(File(packageInfo.applicationInfo.sourceDir))
      val bundleList = if (propsMap.isNullOrEmpty()) {
        emptyList()
      } else {
        propsMap.map { prop ->
          AppPropItem(key = prop.key, value = prop.value?.toString().orEmpty())
        }.sortedBy { item -> item.key }
      }
      root.adapter.setList(bundleList)
    }
  }
}
