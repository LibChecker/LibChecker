package com.absinthe.libchecker.view.app

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.preference.model.PreferenceItemGroupPosition
import com.absinthe.libchecker.ui.preference.model.PreferenceItemRenderState
import com.absinthe.libchecker.ui.preference.view.PreferenceItemView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.manager.SystemBarManager

class PreferenceActionSheetView(context: Context, @StringRes title: Int) : BottomSheetScaffoldView(context) {
  private val items = LinearLayout(context).apply { orientation = VERTICAL }

  init {
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    header.title.setText(title)
    addView(
      NestedScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        addView(items)
      }
    )
  }

  fun addAction(
    key: String,
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    groupPosition: PreferenceItemGroupPosition,
    onClick: () -> Unit
  ) {
    val title = context.getString(titleRes)
    val summary = context.getString(summaryRes)
    val item = LayoutInflater.from(context)
      .inflate(R.layout.preference_m3e, items, false)
      .let { it as PreferenceItemView }
      .apply {
        findViewById<ImageView>(android.R.id.icon).setImageResource(iconRes)
        findViewById<TextView>(android.R.id.title).text = title
        findViewById<TextView>(android.R.id.summary).apply {
          text = summary
          isVisible = true
        }
        bind(
          PreferenceItemRenderState(
            preferenceKey = key,
            title = title,
            summary = summary,
            toggleChecked = null,
            showChevron = true,
            badgeDescription = null,
            groupPosition = groupPosition
          )
        )
        setOnClickListener { onClick() }
      }
    items.addView(item)
  }
}
