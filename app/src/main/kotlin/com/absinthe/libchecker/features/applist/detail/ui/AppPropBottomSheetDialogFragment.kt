package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.bean.AppPropItem
import com.absinthe.libchecker.features.applist.detail.ui.view.AppPropsBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_PACKAGE_INFO = "EXTRA_PACKAGE_INFO"
const val EXTRA_PROPS = "EXTRA_PROPS"

class AppPropBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppPropsBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageInfo by lazy {
    BundleCompat.getParcelable(
      requireArguments(),
      EXTRA_PACKAGE_INFO,
      PackageInfo::class.java
    )
  }

  private val props by lazy {
    requireArguments().getString(EXTRA_PROPS)?.fromJson<Map<String, String>>()
  }

  override fun initRootView(): AppPropsBottomSheetView = AppPropsBottomSheetView(requireContext(), packageInfo)

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    lifecycleScope.launch {
      val propertyList = viewModel.getAppManifestProperties(packageInfo, props)
        .map { property ->
          AppPropItem(
            key = property.key,
            value = property.value
          )
        }
      root.adapter.setList(propertyList)
    }
  }
}
