package com.absinthe.libchecker.domain.app.list.ui.view

import android.content.Context
import android.graphics.Typeface
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.utils.extensions.dp
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class AdvancedMenuSortView(
  context: Context
) : LinearLayout(context),
  MaterialButtonToggleGroup.OnButtonCheckedListener {

  private var currentOptions = AdvancedOptions.DEFAULT_OPTIONS
  private var onDisplayOptionsChanged: (Int) -> Unit = {}
  private var isListening = false

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

  fun bind(
    displayOptions: Int,
    onDisplayOptionsChanged: (Int) -> Unit
  ) {
    currentOptions = displayOptions
    this.onDisplayOptionsChanged = onDisplayOptionsChanged
    val checkedId = currentOptions.toSortButtonId()
    if (toggleGroup.checkedButtonId != checkedId) {
      if (isListening) {
        toggleGroup.removeOnButtonCheckedListener(this)
        isListening = false
      }
      toggleGroup.check(checkedId)
    }
    if (!isListening) {
      toggleGroup.addOnButtonCheckedListener(this)
      isListening = true
    }
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
    if (!isChecked) {
      return
    }
    currentOptions = currentOptions
      .clearSortOptions()
      .or(checkedId.toSortOption())
    onDisplayOptionsChanged(currentOptions)
  }

  private fun Int.toSortButtonId(): Int {
    return when {
      (this and AdvancedOptions.SORT_BY_NAME) > 0 -> R.id.sort_by_name
      (this and AdvancedOptions.SORT_BY_UPDATE_TIME) > 0 -> R.id.sort_by_time
      (this and AdvancedOptions.SORT_BY_TARGET_API) > 0 -> R.id.sort_by_target_version
      else -> R.id.sort_by_name
    }
  }

  private fun Int.clearSortOptions(): Int {
    return this and (
      AdvancedOptions.SORT_BY_NAME or
        AdvancedOptions.SORT_BY_UPDATE_TIME or
        AdvancedOptions.SORT_BY_TARGET_API
      ).inv()
  }

  private fun Int.toSortOption(): Int {
    return when (this) {
      R.id.sort_by_time -> AdvancedOptions.SORT_BY_UPDATE_TIME
      R.id.sort_by_target_version -> AdvancedOptions.SORT_BY_TARGET_API
      else -> AdvancedOptions.SORT_BY_NAME
    }
  }
}
