package com.absinthe.libchecker.features.settings.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.settings.bean.GetUpdatesItem
import com.absinthe.libchecker.features.settings.ui.adapter.GetUpdatesAdapter
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
  private val _adapter = GetUpdatesAdapter()

  private val recyclerView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setPadding(16.dp, 16.dp, 16.dp, 16.dp)
    adapter = _adapter
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

  fun setItems(items: List<GetUpdatesItem>) {
    _adapter.setList(items)
  }
}
