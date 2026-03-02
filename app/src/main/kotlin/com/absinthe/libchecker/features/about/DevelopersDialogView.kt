package com.absinthe.libchecker.features.about

import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class DevelopersDialogView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = "Developers"
  }
  private val _adapter = DeveloperInfoAdapter()

  private val recyclerView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    setPadding(16.dp, 16.dp, 16.dp, 16.dp)
    adapter = _adapter
    layoutManager = LinearLayoutManager(context)
    addItemDecoration(
      VerticalSpacesItemDecoration(4.dp)
    )
  }

  init {
    orientation = VERTICAL
    addView(header)
    addView(recyclerView)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  fun setItems(items: List<DeveloperInfo>) {
    _adapter.setList(items)
  }
}
