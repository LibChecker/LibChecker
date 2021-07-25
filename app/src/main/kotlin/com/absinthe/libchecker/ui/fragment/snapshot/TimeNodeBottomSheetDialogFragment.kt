package com.absinthe.libchecker.ui.fragment.snapshot

import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.ui.fragment.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.snapshot.TimeNodeBottomSheetView

const val EXTRA_TOP_APPS = "EXTRA_TOP_APPS"

class TimeNodeBottomSheetDialogFragment :
  BaseBottomSheetViewDialogFragment<TimeNodeBottomSheetView>() {

  private lateinit var headerView: BottomSheetHeaderView

  private var itemClickAction: ((position: Int) -> Unit)? = null
  private var customTitle: String? = null

  override fun initRootView(): TimeNodeBottomSheetView = TimeNodeBottomSheetView(requireContext())
  override fun getHeaderView(): BottomSheetHeaderView = headerView

  override fun init() {
    headerView = BottomSheetHeaderView(requireContext()).apply {
      layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.bottomMargin = 24.dp
      }
      title.text = getString(R.string.dialog_title_change_timestamp)
    }
    root.adapter.setHeaderView(headerView)
    customTitle?.let { headerView.title.text = it }
    itemClickAction?.let {
      root.adapter.setOnItemClickListener { _, _, position ->
        it(position)
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
