package com.absinthe.libchecker.view.applist

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.recyclerview.SafeFlexboxLayoutManager
import com.absinthe.libchecker.recyclerview.adapter.applist.AdvancedMenuAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent

class AdvancedMenuBSDView(context: Context) : LinearLayout(context), IHeaderView {

  val adapter by unsafeLazy { AdvancedMenuAdapter() }

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  @SuppressLint("SetTextI18n")
  private val demoView = AppItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutline)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSecondaryContainer))
    container.also {
      it.appName.text = "Example"
      it.packageName.text = "this.is.an.example"
      it.icon.load(R.drawable.ic_icon_blueprint)
      it.versionInfo.text = "2020.3.19 (1120)"
      it.abiInfo.text = buildSpannedString {
        inSpans(CenterAlignImageSpan(
          R.drawable.ic_abi_label_64bit.getDrawable(context)!!.also { icon ->
            icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
          }
        )) {
          append(" ")
        }
        append(" ")
        append(context.getString(R.string.arm64_v8a))
        append(", API")
        append(Build.VERSION.SDK_INT.toString())
      }
    }
  }

  private val list = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    adapter = this@AdvancedMenuBSDView.adapter
    layoutManager = SafeFlexboxLayoutManager(context).also {
      it.flexDirection = FlexDirection.ROW
      it.justifyContent = JustifyContent.FLEX_START
      it.alignContent
    }
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(demoView)
    addView(list)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
