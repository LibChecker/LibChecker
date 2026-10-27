package com.absinthe.libchecker.domain.snapshot.list.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailDiffTextStyle
import com.absinthe.libchecker.domain.snapshot.detail.repository.SnapshotDetailPayloadRepository
import com.absinthe.libchecker.domain.snapshot.detail.ui.EXTRA_PAYLOAD_ID
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotNoDiffTitleIconRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toSnapshotNoDiffRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toSnapshotNoDiffTitleIconRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotNoDiffBSView
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotTitleDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.putArguments
import com.google.android.material.R as MaterialR
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class SnapshotNoDiffBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotNoDiffBSView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private val buildSnapshotTitleDisplayData: BuildSnapshotTitleDisplayDataUseCase by inject()
  private val payloadRepository: SnapshotDetailPayloadRepository by inject()

  override fun initRootView(): SnapshotNoDiffBSView = SnapshotNoDiffBSView(requireContext())

  override fun init() = Unit

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
      val item = try {
        arguments?.getString(EXTRA_PAYLOAD_ID)?.let { payloadRepository.load(it) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Timber.e(e, "Failed to load snapshot popup")
        null
      }
      if (item == null) {
        dismissAllowingStateLoss()
        return@launch
      }
      val metricColor = requireContext().getColorByAttr(MaterialR.attr.colorOnSurface)
      val diffTextStyle = SnapshotDetailDiffTextStyle(
        highlightColor = if ((viewModel.getSnapshotOptions() and SnapshotOptions.DIFF_HIGHLIGHT) > 0) {
          requireContext().getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
        } else {
          null
        },
        emphasizeDiffs = (viewModel.getSnapshotOptions() and SnapshotOptions.DIFF_EMPHASIS) > 0,
        arrowColor = metricColor,
        metricDeltaColor = metricColor
      )
      val titleRenderState = buildSnapshotTitleDisplayData(
        BuildSnapshotTitleDisplayDataUseCase.Request(
          item = item,
          formatSplitPackageName = false,
          diffTextStyle = diffTextStyle
        )
      )
      val renderState = item.toSnapshotNoDiffRenderState(titleRenderState) ?: run {
        dismissAllowingStateLoss()
        return@launch
      }
      root.render(renderState)
      bindIcon(item)
    }
  }

  private fun bindIcon(item: SnapshotDiffItem) {
    root.renderTitleIcon(SnapshotNoDiffTitleIconRenderState.Fallback)
    viewLifecycleOwner.lifecycleScope.launch {
      val iconState = viewModel.getSnapshotPackageIconSources(listOf(item.packageName))[item.packageName]
        .toSnapshotNoDiffTitleIconRenderState()
      root.renderTitleIcon(iconState) {
        viewLifecycleOwner.lifecycleScope.launch {
          val lcItem = viewModel.getAppListItem(item.packageName) ?: return@launch
          activity?.launchDetailPage(lcItem)
        }
      }
    }
  }

  companion object {
    fun newInstance(payloadId: String): SnapshotNoDiffBSDFragment {
      return SnapshotNoDiffBSDFragment().putArguments(
        EXTRA_PAYLOAD_ID to payloadId
      )
    }
  }
}
