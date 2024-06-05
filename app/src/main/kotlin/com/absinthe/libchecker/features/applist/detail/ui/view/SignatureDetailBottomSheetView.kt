package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.ui.adapter.SignatureDetailAdapter
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class SignatureDetailBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  val adapter by unsafeLazy { SignatureDetailAdapter() }

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.signature_detail)
  }

  val icon = AppCompatImageView(context).apply {
    val iconSize = 48.dp
    layoutParams = LayoutParams(iconSize, iconSize).also {
      it.topMargin = 4.dp
      it.gravity = Gravity.CENTER_HORIZONTAL
    }
    setImageResource(R.drawable.ic_signatures)
    setBackgroundResource(R.drawable.bg_circle_outline)
  }

  val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 12.dp
    }
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    adapter = this@SignatureDetailBottomSheetView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
    setHasFixedSize(true)
    addItemDecoration(VerticalSpacesItemDecoration(4.dp))
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(icon)
    addView(list)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
