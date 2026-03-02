package com.absinthe.libchecker.features.applist.ui.view

import android.content.Context
import android.graphics.Typeface
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.utils.extensions.dp
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class AdvancedMenuSortView(context: Context) :
  LinearLayout(context),
  MaterialButtonToggleGroup.OnButtonCheckedListener {

  private val title = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    )
    setTypeface(null, Typeface.BOLD)
    text = context.getString(R.string.adv_sort_mode)
  }

  private val toggleGroup = MaterialButtonToggleGroup(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    )
    isSelectionRequired = true
    isSingleSelection = true
    addOnButtonCheckedListener(this@AdvancedMenuSortView)

    if ((GlobalValues.advancedOptions and AdvancedOptions.SORT_BY_NAME) > 0) {
      check(R.id.sort_by_name)
    } else if ((GlobalValues.advancedOptions and AdvancedOptions.SORT_BY_UPDATE_TIME) > 0) {
      check(R.id.sort_by_time)
    } else if ((GlobalValues.advancedOptions and AdvancedOptions.SORT_BY_TARGET_API) > 0) {
      check(R.id.sort_by_target_version)
    }
  }

  private val group = HorizontalScrollView(context).apply {
    isHorizontalScrollBarEnabled = false
    layoutParams = LayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    addView(toggleGroup)
  }

  init {
    orientation = VERTICAL
    addView(title)
    addView(group)
    addButton(context, R.string.adv_sort_by_name, R.id.sort_by_name)
    addButton(context, R.string.adv_sort_by_time, R.id.sort_by_time)
    addButton(context, R.string.adv_sort_by_target_version, R.id.sort_by_target_version)
  }

  private fun addButton(context: Context, titleRes: Int, viewId: Int) {
    val chip = MaterialButton(
      ContextThemeWrapper(context, R.style.App_Widget_AdvancedMenuToggle)
    ).apply {
      id = viewId
      layoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT
      )
      text = context.getString(titleRes)
    }
    toggleGroup.addView(chip)
  }

  override fun onButtonChecked(
    group: MaterialButtonToggleGroup?,
    checkedId: Int,
    isChecked: Boolean
  ) {
    var options = GlobalValues.advancedOptions
    when (checkedId) {
      R.id.sort_by_name -> {
        options = if (isChecked) {
          options or AdvancedOptions.SORT_BY_NAME
        } else {
          options and AdvancedOptions.SORT_BY_NAME.inv()
        }
      }

      R.id.sort_by_time -> {
        options = if (isChecked) {
          options or AdvancedOptions.SORT_BY_UPDATE_TIME
        } else {
          options and AdvancedOptions.SORT_BY_UPDATE_TIME.inv()
        }
      }

      R.id.sort_by_target_version -> {
        options = if (isChecked) {
          options or AdvancedOptions.SORT_BY_TARGET_API
        } else {
          options and AdvancedOptions.SORT_BY_TARGET_API.inv()
        }
      }
    }
    GlobalValues.advancedOptions = options
  }
}
