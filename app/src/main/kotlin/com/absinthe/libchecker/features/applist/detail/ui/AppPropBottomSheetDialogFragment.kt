package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import androidx.core.os.BundleCompat
import com.absinthe.libchecker.features.applist.detail.bean.AppPropItem
import com.absinthe.libchecker.features.applist.detail.ui.view.AppPropsBottomSheetView
import com.absinthe.libchecker.utils.extensions.getDexFileOptimizationInfo
import com.absinthe.libchecker.utils.manifest.ApplicationReader
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.io.File
import pxb.android.axml.ValueWrapper

const val EXTRA_PACKAGE_INFO = "EXTRA_PACKAGE_INFO"

class AppPropBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppPropsBottomSheetView>() {

  private val packageInfo by lazy {
    BundleCompat.getParcelable(
      requireArguments(),
      EXTRA_PACKAGE_INFO,
      PackageInfo::class.java
    )!!
  }

  override fun initRootView(): AppPropsBottomSheetView = AppPropsBottomSheetView(requireContext(), packageInfo)

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    val propsMap = runCatching {
      ApplicationReader.getManifestProperties(File(packageInfo.applicationInfo!!.sourceDir))
    }.getOrNull()
    val bundleList = if (propsMap.isNullOrEmpty()) {
      emptyList()
    } else {
      propsMap.map { prop ->
        AppPropItem(
          key = prop.key,
          value = when (val value = prop.value) {
            is ValueWrapper -> value.ref.toString()
            else -> value?.toString().orEmpty()
          }
        )
      }.sortedBy { item -> item.key }
    }.toMutableList()

    packageInfo.getDexFileOptimizationInfo()?.let {
      bundleList.add(
        0,
        AppPropItem(
          key = "dexopt",
          value = "status=${it.status}, reason=${it.reason}"
        )
      )
    }

    root.adapter.setList(bundleList)
  }
}
