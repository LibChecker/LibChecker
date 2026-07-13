package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightContent
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightField
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator

class LibraryExtraInfoCardView(context: Context) : MaterialCardView(context) {

  private val content = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(12.dp, 10.dp, 12.dp, 10.dp)
  }
  private val statusRow = LinearLayout(context).apply {
    gravity = Gravity.CENTER_VERTICAL
    orientation = LinearLayout.HORIZONTAL
  }
  private val progress = CircularProgressIndicator(context).apply {
    layoutParams = LinearLayout.LayoutParams(16.dp, 16.dp).also { it.marginEnd = 8.dp }
    isIndeterminate = true
    indicatorSize = 16.dp
    trackThickness = 2.dp
  }
  private val statusText = AppCompatTextView(context).apply {
    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodySmall))
  }
  private val summaryContainer = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
  }
  private val detailsContainer = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    isVisible = false
  }
  private val expandText = AppCompatTextView(context).apply {
    gravity = Gravity.END
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
    setTextColor(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
    isVisible = false
  }
  private var expanded = false
  private var retryAction: (() -> Unit)? = null
  private var currentContent: LibraryInsightContent? = null

  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    minimumHeight = 48.dp
    setSmoothRoundCorner(12.dp)
    strokeWidth = 1.dp
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainer))
    cardElevation = 0f
    isVisible = false
    statusRow.addView(progress)
    statusRow.addView(statusText)
    content.addView(statusRow)
    content.addView(summaryContainer)
    content.addView(detailsContainer)
    content.addView(expandText)
    addView(content)
    setOnClickListener { handleClick() }
  }

  fun render(state: LibraryInsightUiState, onRetry: () -> Unit) {
    retryAction = onRetry
    when (state) {
      LibraryInsightUiState.Hidden -> {
        isVisible = false
        isClickable = false
        isFocusable = false
        currentContent = null
        ViewCompat.setStateDescription(this, null)
      }

      LibraryInsightUiState.Loading -> showLoading()

      LibraryInsightUiState.Unavailable -> showUnavailable()

      is LibraryInsightUiState.Content -> showContent(state.content)
    }
  }

  private fun showLoading() {
    isVisible = true
    currentContent = null
    isClickable = false
    isFocusable = false
    progress.isVisible = true
    statusRow.isVisible = true
    statusText.text = context.getString(R.string.library_insight_loading)
    statusText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
    summaryContainer.removeAllViews()
    detailsContainer.removeAllViews()
    detailsContainer.isVisible = false
    expandText.isVisible = false
    contentDescription = statusText.text
    ViewCompat.setStateDescription(this, null)
  }

  private fun showUnavailable() {
    isVisible = true
    currentContent = null
    isClickable = true
    isFocusable = true
    progress.isVisible = false
    statusRow.isVisible = true
    statusText.text = context.getString(R.string.library_insight_unavailable)
    statusText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_refresh, 0, 0, 0)
    statusText.compoundDrawablePadding = 8.dp
    TextViewCompat.setCompoundDrawableTintList(
      statusText,
      ColorStateList.valueOf(context.getColorByAttr(android.R.attr.colorControlNormal))
    )
    summaryContainer.removeAllViews()
    detailsContainer.removeAllViews()
    detailsContainer.isVisible = false
    expandText.isVisible = false
    contentDescription = statusText.text
    ViewCompat.setStateDescription(this, null)
  }

  private fun showContent(content: LibraryInsightContent) {
    isVisible = true
    currentContent = content
    expanded = false
    progress.isVisible = false
    statusRow.isVisible = false
    summaryContainer.replaceFields(content.summary)
    detailsContainer.replaceFields(content.details)
    detailsContainer.isVisible = false
    expandText.isVisible = content.details.isNotEmpty()
    expandText.text = context.getString(R.string.library_insight_show_details)
    isClickable = content.details.isNotEmpty()
    isFocusable = isClickable
    updateContentDescription(content)
  }

  private fun handleClick() {
    val content = currentContent
    if (content == null) {
      retryAction?.invoke()
      return
    }
    if (content.details.isEmpty()) return
    expanded = !expanded
    detailsContainer.isVisible = expanded
    expandText.text = context.getString(
      if (expanded) R.string.library_insight_hide_details else R.string.library_insight_show_details
    )
    updateContentDescription(content)
  }

  private fun LinearLayout.replaceFields(fields: List<LibraryInsightField>) {
    removeAllViews()
    fields.forEachIndexed { index, field ->
      addView(
        createFieldRow(field).apply {
          if (index > 0) setPadding(0, 4.dp, 0, 0)
        }
      )
    }
  }

  private fun createFieldRow(field: LibraryInsightField): View {
    val row = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.TOP
    }
    val label = AppCompatTextView(context).apply {
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.38f).also {
        it.marginEnd = 8.dp
      }
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      text = field.label
    }
    val value = AppCompatTextView(
      ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)
    ).apply {
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.62f)
      gravity = Gravity.END
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      text = field.displayValue()
    }
    row.addView(label)
    row.addView(value)
    return row
  }

  private fun LibraryInsightField.displayValue(): String {
    val hiddenCount = (totalCount - values.size).coerceAtLeast(0)
    return buildString {
      append(values.joinToString(" / "))
      if (hiddenCount > 0) append(" +").append(hiddenCount)
    }
  }

  private fun updateContentDescription(content: LibraryInsightContent) {
    val visibleFields = if (expanded) content.summary + content.details else content.summary
    contentDescription = visibleFields.joinToString(", ") { "${it.label}: ${it.displayValue()}" }
    ViewCompat.setStateDescription(
      this,
      if (content.details.isEmpty()) {
        null
      } else {
        context.getString(if (expanded) R.string.a11y_state_expanded else R.string.a11y_state_collapsed)
      }
    )
  }
}
