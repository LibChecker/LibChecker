package com.absinthe.libchecker.domain.snapshot.list.ui

import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotNoDiffBSView
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotTitleDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_DIFF_ITEM = "EXTRA_DIFF_ITEM"

class SnapshotNoDiffBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotNoDiffBSView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private val buildSnapshotTitleDisplayData: BuildSnapshotTitleDisplayDataUseCase by inject()

  override fun initRootView(): SnapshotNoDiffBSView = SnapshotNoDiffBSView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val arg = arguments ?: run {
      dismiss()
      return
    }
    BundleCompat.getSerializable(arg, EXTRA_DIFF_ITEM, SnapshotDiffItem::class.java)?.let { item ->
      root.title.apply {
        bindIcon(item)
        render(
          buildSnapshotTitleDisplayData(
            BuildSnapshotTitleDisplayDataUseCase.Request(
              item = item,
              formatSplitPackageName = false
            )
          ).toRenderState(copyPrimaryText = false)
        )
      }

      when {
        item.newInstalled -> {
          root.setMode(SnapshotNoDiffBSView.Mode.New)
        }

        item.deleted -> {
          root.setMode(SnapshotNoDiffBSView.Mode.Deleted)
        }

        item.isNothingChanged() -> {
          root.setMode(SnapshotNoDiffBSView.Mode.NothingChanged)
        }

        else -> {
          dismiss()
        }
      }
    } ?: run {
      dismiss()
    }
  }

  private fun bindIcon(item: SnapshotDiffItem) {
    root.title.setFallbackIcon()
    root.title.setIconClickListener(null)
    lifecycleScope.launch {
      when (val iconSource = viewModel.getSnapshotPackageIconSources(listOf(item.packageName))[item.packageName]) {
        null -> root.title.setFallbackIcon()

        else -> {
          root.title.setIconSource(iconSource)
          root.title.setIconClickListener {
            lifecycleScope.launch {
              val lcItem = viewModel.getAppListItem(item.packageName) ?: return@launch
              activity?.launchDetailPage(lcItem)
            }
          }
        }
      }
    }
  }

  companion object {
    fun newInstance(snapshotDiffItem: SnapshotDiffItem): SnapshotNoDiffBSDFragment {
      return SnapshotNoDiffBSDFragment().putArguments(
        EXTRA_DIFF_ITEM to snapshotDiffItem
      )
    }
  }
}
