package com.absinthe.libchecker.domain.settings.ui

import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.settings.model.GetUpdatesAction
import com.absinthe.libchecker.domain.settings.model.GetUpdatesDialogState
import com.absinthe.libchecker.domain.settings.ui.adapter.GetUpdatesAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class GetUpdatesDialogView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.settings_get_updates)
  }
  private var onAction: (GetUpdatesAction) -> Unit = {}
  private val adapter = GetUpdatesAdapter { onAction(it) }

  private val recyclerView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    setPadding(16.dp, 16.dp, 16.dp, 16.dp)
    adapter = this@GetUpdatesDialogView.adapter
    layoutManager = LinearLayoutManager(context)
  }

  init {
    orientation = VERTICAL
    addView(header)
    addView(recyclerView)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  fun bind(
    state: GetUpdatesDialogState,
    onAction: (GetUpdatesAction) -> Unit
  ) {
    this.onAction = onAction
    adapter.setList(state.items)
  }
}
