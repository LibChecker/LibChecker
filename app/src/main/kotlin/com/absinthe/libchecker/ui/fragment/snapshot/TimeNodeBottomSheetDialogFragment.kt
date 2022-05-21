package com.absinthe.libchecker.ui.fragment.snapshot

import android.view.ViewGroup
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.view.detail.EmptyListView
import com.absinthe.libchecker.view.snapshot.TimeNodeBottomSheetView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

const val EXTRA_TOP_APPS = "EXTRA_TOP_APPS"

class TimeNodeBottomSheetDialogFragment :
  BaseBottomSheetViewDialogFragment<TimeNodeBottomSheetView>() {

  private var itemClickAction: ((position: Int) -> Unit)? = null
  private var customTitle: String? = null

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
      }
    }

    arguments?.getParcelableArrayList<TimeStampItem>(EXTRA_TOP_APPS)?.let { topApps ->
      root.adapter.setList(topApps)
    }
  }

  fun setTitle(title: String) {
    customTitle = title
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
