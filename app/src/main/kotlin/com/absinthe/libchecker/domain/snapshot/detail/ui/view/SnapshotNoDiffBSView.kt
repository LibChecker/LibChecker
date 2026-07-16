package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotNoDiffMode
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotNoDiffRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotNoDiffTitleIconRenderState
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class SnapshotNoDiffBSView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.detail)
  }

  private val title = SnapshotTitleView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
  }

  private var stubView: View? = null

  init {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    orientation = VERTICAL
    setPadding(24.dp, 16.dp, 24.dp, 0)
    addView(header)
    addView(title)
  }

  fun render(state: SnapshotNoDiffRenderState) {
    title.render(state.title)
    setMode(state.mode)
  }

  fun renderTitleIcon(
    state: SnapshotNoDiffTitleIconRenderState,
    onClickListener: OnClickListener? = null
  ) {
    title.setIconSource(state.iconSource)
    title.setIconClickListener(onClickListener.takeIf { state.opensDetailOnClick })
  }

  private fun setMode(mode: SnapshotNoDiffMode) {
    stubView?.let {
      if (it.parent != null) {
        (it.parent as ViewGroup).removeView(it)
      }
    }
    when (mode) {
      SnapshotNoDiffMode.New -> {
        stubView = SnapshotDetailNewInstallView(context).apply {
          layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
      }

      SnapshotNoDiffMode.Deleted -> {
        stubView = SnapshotDetailDeletedView(context).apply {
          layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
      }

      SnapshotNoDiffMode.NothingChanged -> {
        stubView = SnapshotEmptyView(context).apply {
          layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
      }
    }
    addView(stubView)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
