package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.content.Context
import android.widget.LinearLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceMenuAction
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceMenuBottomSheetState
import com.absinthe.libchecker.ui.app.MenuOptionItemView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class LibReferenceMenuBSDView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private var onAction: (LibReferenceMenuAction) -> Unit = {}

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  private val optionsLayout = FlexboxLayout(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
      it.bottomMargin = 8.dp
    }
    flexWrap = FlexWrap.WRAP
    justifyContent = JustifyContent.FLEX_START
    flexDirection = FlexDirection.ROW
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(optionsLayout)
  }

  fun bind(
    state: LibReferenceMenuBottomSheetState,
    onAction: (LibReferenceMenuAction) -> Unit
  ) {
    this.onAction = onAction
    optionsLayout.removeAllViews()
    state.options.forEach { item ->
      optionsLayout.addView(
        MenuOptionItemView(
          context = context,
          startMarginDp = 4,
          endMarginDp = 4
        ).apply {
          bind(item) { isChecked ->
            this@LibReferenceMenuBSDView.onAction(
              LibReferenceMenuAction.OptionChanged(
                item = item,
                isChecked = isChecked
              )
            )
          }
        }
      )
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = header

  override fun onDetachedFromWindow() {
    onAction = {}
    super.onDetachedFromWindow()
  }
}
