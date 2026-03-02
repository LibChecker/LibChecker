package com.absinthe.libchecker.features.snapshot.detail.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.absinthe.libchecker.R
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

  val title = SnapshotTitleView(context).apply {
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

  fun setMode(mode: Mode) {
    stubView?.let {
      if (it.parent != null) {
        (it.parent as ViewGroup).removeView(it)
      }
    }
    when (mode) {
      Mode.New -> {
        stubView = SnapshotDetailNewInstallView(context).apply {
          layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
      }

      Mode.Deleted -> {
        stubView = SnapshotDetailDeletedView(context).apply {
          layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
      }

      Mode.NothingChanged -> {
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

  sealed class Mode {
    data object New : Mode()
    data object Deleted : Mode()
    data object NothingChanged : Mode()
  }
}
