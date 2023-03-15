package com.absinthe.libchecker.ui.fragment.snapshot

import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.absinthe.libchecker.compat.BundleCompat
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.view.detail.EmptyListView
import com.absinthe.libchecker.view.snapshot.TimeNodeAddApkView
import com.absinthe.libchecker.view.snapshot.TimeNodeBottomSheetView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

const val EXTRA_TOP_APPS = "EXTRA_TOP_APPS"

class TimeNodeBottomSheetDialogFragment :
  BaseBottomSheetViewDialogFragment<TimeNodeBottomSheetView>() {

  private val viewModel: SnapshotViewModel by activityViewModels()
  private var itemClickAction: ((position: Int) -> Unit)? = null
  private var customTitle: String? = null
  private var isCompareMode: Boolean = false
  private var isLeftMode: Boolean = false

  override fun initRootView(): TimeNodeBottomSheetView = TimeNodeBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.post {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.67).toInt()
    }
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
      BundleCompat.getParcelableArrayList<TimeStampItem>(it, EXTRA_TOP_APPS)?.let { topApps ->
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
