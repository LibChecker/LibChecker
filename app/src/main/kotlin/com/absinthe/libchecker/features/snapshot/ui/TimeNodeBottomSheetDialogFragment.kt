package com.absinthe.libchecker.features.snapshot.ui

import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.activityViewModels
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.features.snapshot.ui.view.TimeNodeAddApkView
import com.absinthe.libchecker.features.snapshot.ui.view.TimeNodeBottomSheetView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

const val EXTRA_TOP_APPS = "EXTRA_TOP_APPS"

class TimeNodeBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<TimeNodeBottomSheetView>() {

  private val viewModel: SnapshotViewModel by activityViewModels()
  private var itemClickAction: ((position: Int) -> Unit)? = null
  private var customTitle: String? = null
  private var isCompareMode: Boolean = false
  private var isLeftMode: Boolean = false

  override fun initRootView(): TimeNodeBottomSheetView = TimeNodeBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    customTitle?.let { getHeaderView().title.text = it }
    itemClickAction?.let {
      root.adapter.apply {
        setOnItemClickListener { _, _, position ->
          it(position)
        }
        setEmptyView(
          EmptyListView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
              it.bottomMargin = 16.dp
            }
          }
        )
        if (isCompareMode) {
          root.adapter.removeAllHeaderView()
          root.adapter.addHeaderView(
            TimeNodeAddApkView(requireContext()).also {
              it.setOnClickListener {
                viewModel.chooseComparedApk(isLeftMode)
                dismiss()
              }
            }
          )
        } else {
          root.adapter.removeAllHeaderView()
        }
      }
    }

    arguments?.let {
      BundleCompat.getParcelableArrayList(it, EXTRA_TOP_APPS, TimeStampItem::class.java)
        ?.let { topApps ->
          root.adapter.setList(topApps)
        }
    }
  }

  fun setTitle(title: String) {
    customTitle = title
  }

  fun setCompareMode(isCompareMode: Boolean) {
    this.isCompareMode = isCompareMode
  }

  fun setLeftMode(isLeftMode: Boolean) {
    this.isLeftMode = isLeftMode
  }

  fun setOnItemClickListener(action: (position: Int) -> Unit) {
    itemClickAction = action
  }

  companion object {
    fun newInstance(topApps: ArrayList<TimeStampItem>): TimeNodeBottomSheetDialogFragment {
      return TimeNodeBottomSheetDialogFragment().putArguments(
        EXTRA_TOP_APPS to topApps
      )
    }
  }
}
