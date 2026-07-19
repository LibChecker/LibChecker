package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticAnalysisState
import com.absinthe.libchecker.domain.app.detail.ui.view.AppStatisticAnalysisBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AppStatisticAnalysisBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppStatisticAnalysisBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private var analysisAllowed = true

  override fun initRootView(): AppStatisticAnalysisBottomSheetView {
    return AppStatisticAnalysisBottomSheetView(requireContext())
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.75f
    root.bind(AppStatisticAnalysisState.Loading(0))
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.appStatisticAnalysisState.collect(root::bind)
      }
    }
    if (viewModel.isPackageInfoAvailable()) {
      viewModel.analyzeOnlineStatistics()
    } else {
      lifecycleScope.launch {
        viewModel.packageInfoStateFlow.filterNotNull().first()
        if (analysisAllowed) {
          viewModel.analyzeOnlineStatistics()
        }
      }
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    analysisAllowed = false
    viewModel.cancelOnlineStatisticAnalysis()
    super.onDismiss(dialog)
  }
}
