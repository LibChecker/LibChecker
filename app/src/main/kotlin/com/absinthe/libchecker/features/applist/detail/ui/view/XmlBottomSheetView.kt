package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class XmlBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val adapter = Adapter()

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.xml_detail)
  }

  private val container = BottomSheetRecyclerView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    adapter = this@XmlBottomSheetView.adapter
    layoutManager = LinearLayoutManager(context)
    overScrollMode = View.OVER_SCROLL_NEVER
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
  }

  init {
    orientation = VERTICAL
    addView(header)
    addView(container)
  }

  fun setText(text: CharSequence?) {
    adapter.setList(listOf(text, ""))
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  class Adapter : BaseQuickAdapter<CharSequence?, BaseViewHolder>(0) {

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
      return createBaseViewHolder(
        AppCompatTextView(context).apply {
          layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          textSize = 10f
          setTextIsSelectable(true)
        }
      )
    }

    override fun convert(holder: BaseViewHolder, item: CharSequence?) {
      (holder.itemView as AppCompatTextView).text = item
    }
  }
}
