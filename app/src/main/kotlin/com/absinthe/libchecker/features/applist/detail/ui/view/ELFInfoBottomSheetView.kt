package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibDetailItemAdapter
import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.LibDetailItem
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class ELFInfoBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.lib_detail_elf_info)
  }

  val icon = AppCompatImageView(context).apply {
    val iconSize = 48.dp
    layoutParams = LayoutParams(iconSize, iconSize).also {
      it.topMargin = 4.dp
    }
    setBackgroundResource(R.drawable.bg_circle_outline)
  }

  val title = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams = LayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 4.dp
    }
    gravity = Gravity.CENTER
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  fun setContent(deps: String, entryPoints: String) {
    val list = listOf(
      LibDetailItem(
        iconRes = R.drawable.ic_content,
        tipRes = R.string.lib_detail_dependency_tip,
        textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2),
        text = deps.takeIf { it.isNotEmpty() } ?: context.getString(R.string.empty_list)
      ),
      LibDetailItem(
        iconRes = R.drawable.ic_content,
        tipRes = R.string.lib_detail_entry_points_tip,
        textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2),
        text = entryPoints.takeIf { it.isNotEmpty() } ?: context.getString(R.string.empty_list)
      )
    )
    contentAdapter.setList(list)
  }

  private val contentAdapter = LibDetailItemAdapter()

  private val contentView = BottomSheetRecyclerView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    )
    adapter = contentAdapter
    overScrollMode = OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
    addItemDecoration(VerticalSpacesItemDecoration(4.dp))
  }

  init {
    orientation = VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    addView(header)
    addView(icon)
    addView(title)
    addView(contentView)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
