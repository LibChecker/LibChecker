package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.view.Gravity
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.SignatureDetailItem
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.ui.adapter.addSpacingDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView

class SignatureDetailBottomSheetView(context: Context) : BottomSheetScaffoldView(context) {

  private val adapter = BindOnlyAdapter(::SignatureDetailItemView, SignatureDetailItemView::bind)

  private val icon = AppCompatImageView(context).apply {
    val iconSize = 48.dp
    layoutParams = LayoutParams(iconSize, iconSize).also {
      it.gravity = Gravity.CENTER_HORIZONTAL
    }
    setImageResource(R.drawable.ic_signatures)
    setBackgroundResource(R.drawable.bg_circle_outline)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 12.dp
    }
    setPadding(16.dp, 0, 16.dp, 0)
    overScrollMode = OVER_SCROLL_NEVER
    adapter = this@SignatureDetailBottomSheetView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    setHasFixedSize(true)
    addSpacingDecoration(4.dp)
  }

  init {
    setPadding(0, 16.dp, 0, 0)
    header.title.text = context.getString(R.string.signature_detail)
    addView(icon)
    addView(list)
  }

  fun bind(
    items: List<SignatureDetailItem>,
    onItemLongClick: () -> Unit
  ) {
    adapter.setOnItemLongClickListener { _, _, _ ->
      onItemLongClick()
      true
    }
    adapter.setList(items)
  }
}
