package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView

class XmlBottomSheetView(context: Context) : BottomSheetScaffoldView(context) {

  private val adapter = BindOnlyAdapter<CharSequence, AppCompatTextView>(
    viewFactory = { context ->
      AppCompatTextView(context).apply {
        layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        textSize = 10f
        setTextIsSelectable(true)
      }
    },
    bindView = { text = it }
  )

  private val container = BottomSheetRecyclerView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    adapter = this@XmlBottomSheetView.adapter
    layoutManager = LinearLayoutManager(context)
    overScrollMode = OVER_SCROLL_NEVER
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
  }

  init {
    header.title.text = context.getString(R.string.xml_detail)
    addContentView(container)
  }

  fun setText(text: CharSequence?) {
    adapter.setList(listOf(text ?: "", ""))
  }
}
