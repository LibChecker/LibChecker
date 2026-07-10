package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.pm.PackageInfo
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_INFO
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.view.AppBundleBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AppBundleBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppBundleBottomSheetView>() {

  private val packageInfo by lazy { BundleCompat.getParcelable(requireArguments(), EXTRA_PACKAGE_INFO, PackageInfo::class.java) }
  private val viewModel: DetailViewModel by activityViewModel()

  override fun initRootView(): AppBundleBottomSheetView = AppBundleBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    packageInfo?.let { pi ->
      lifecycleScope.launch {
        root.bind(viewModel.getAppBundleItems(pi))
      }
    }
  }
}
