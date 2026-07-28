package com.absinthe.libchecker.domain.statistics.chart.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class StatisticCatalogEditorBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<StatisticCatalogEditorView>() {

  private val viewModel: ChartViewModel by activityViewModel()

  override fun initRootView(): StatisticCatalogEditorView = StatisticCatalogEditorView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.9f
    root.bind(viewModel.statisticCatalogEditorState.value, ::handleAction)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.statisticCatalogEditorState.collect { state ->
          root.bind(state, ::handleAction)
        }
      }
    }
    viewModel.openStatisticCatalogEditor()
  }

  private fun handleAction(action: StatisticCatalogEditorAction) {
    when (action) {
      is StatisticCatalogEditorAction.Add -> viewModel.addStatistic(action.statistic)

      is StatisticCatalogEditorAction.Remove -> viewModel.removeStatistic(action.statisticId)

      is StatisticCatalogEditorAction.Move -> viewModel.moveStatistic(
        action.fromIndex,
        action.toIndex
      )
    }
  }
}
