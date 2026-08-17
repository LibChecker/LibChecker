package com.absinthe.libchecker.domain.app.detail.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightContent
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightField
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator

class LibraryExtraInfoCardView(
  context: Context,
  private val onExpansionAnimationStateChange: (Boolean) -> Unit = {}
) : MaterialCardView(context) {

  private val content = LinearLayout(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    minimumHeight = 48.dp
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER_VERTICAL
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
    layoutParams = LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    orientation = LinearLayout.VERTICAL
  }
  private val detailsContainer = LinearLayout(context).apply {
    layoutParams = LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    orientation = LinearLayout.VERTICAL
    setPadding(0, 8.dp, 0, 0)
    isVisible = false
  }
  private val expandText = AppCompatTextView(context).apply {
    layoutParams = LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 6.dp
    }
    gravity = Gravity.END or Gravity.CENTER_VERTICAL
    minimumHeight = 24.dp
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
    setTextColor(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
    isVisible = false
  }
  private var expanded = false
  private var expansionAnimator: ValueAnimator? = null
  private var retryAction: (() -> Unit)? = null
  private var currentContent: LibraryInsightContent? = null

  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    minimumWidth = COMPACT_MIN_WIDTH
    minimumHeight = 48.dp
    strokeWidth = 0
    setCardBackgroundColor(Color.TRANSPARENT)
    rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
    cardElevation = 0f
    stateListAnimator = null
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
    val minimumCardWidth = if (content.details.isEmpty()) {
      COMPACT_MIN_WIDTH
    } else {
      EXPANDABLE_MIN_WIDTH
    }
    minimumWidth = minimumCardWidth
    this.content.minimumWidth = minimumCardWidth
    currentContent = content
    expanded = false
    progress.isVisible = false
    statusRow.isVisible = false
    summaryContainer.replaceSummaryFields(content.summary)
    detailsContainer.replaceFields(content.details, technical = true)
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
    if (expansionAnimator?.isRunning == true) return
    animateDetails(shouldExpand = !expanded)
  }

  private fun animateDetails(shouldExpand: Boolean) {
    onExpansionAnimationStateChange(true)
    expanded = shouldExpand
    detailsContainer.isVisible = true
    val startHeight = if (shouldExpand) 0 else detailsContainer.height
    val endHeight = if (shouldExpand) {
      val availableWidth = (
        content.width -
          content.paddingStart -
          content.paddingEnd
        ).coerceAtLeast(0)
      detailsContainer.measure(
        MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
      )
      detailsContainer.measuredHeight
    } else {
      0
    }
    detailsContainer.layoutParams = detailsContainer.layoutParams.apply {
      height = startHeight
    }
    detailsContainer.alpha = if (shouldExpand) 0f else 1f
    expandText.text = context.getString(
      if (expanded) R.string.library_insight_hide_details else R.string.library_insight_show_details
    )
    expansionAnimator = ValueAnimator.ofInt(startHeight, endHeight).apply {
      duration = EXPANSION_DURATION
      interpolator = FastOutSlowInInterpolator()
      addUpdateListener { animator ->
        detailsContainer.layoutParams = detailsContainer.layoutParams.apply {
          height = animator.animatedValue as Int
        }
        detailsContainer.alpha = if (shouldExpand) {
          animator.animatedFraction
        } else {
          1f - animator.animatedFraction
        }
        detailsContainer.requestLayout()
      }
      addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            detailsContainer.layoutParams = detailsContainer.layoutParams.apply {
              height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            detailsContainer.isVisible = shouldExpand
            detailsContainer.alpha = 1f
            expansionAnimator = null
            currentContent?.let(::updateContentDescription)
            detailsContainer.requestLayout()
            post { onExpansionAnimationStateChange(false) }
          }
        }
      )
      start()
    }
  }

  private fun LinearLayout.replaceSummaryFields(fields: List<LibraryInsightField>) {
    removeAllViews()
    fields.firstOrNull()?.let { field ->
      addView(createPrimaryField(field))
    }
    fields.drop(1).forEachIndexed { index, field ->
      addView(
        createFieldRow(field, technical = false).apply {
          setPadding(0, if (index == 0) 14.dp else 6.dp, 0, 0)
        }
      )
    }
  }

  private fun createPrimaryField(field: LibraryInsightField): View {
    return LinearLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      gravity = Gravity.CENTER_HORIZONTAL
      orientation = LinearLayout.VERTICAL
      addView(
        AppCompatTextView(context).apply {
          layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          gravity = Gravity.CENTER
          includeFontPadding = false
          setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
          setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
          text = field.label
        }
      )
      addView(
        AppCompatTextView(
          ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)
        ).apply {
          layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          ).also {
            it.topMargin = 4.dp
          }
          gravity = Gravity.CENTER
          includeFontPadding = false
          setTextColor(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
          text = field.displayValue()
        }
      )
    }
  }

  private fun LinearLayout.replaceFields(
    fields: List<LibraryInsightField>,
    technical: Boolean
  ) {
    removeAllViews()
    fields.forEachIndexed { index, field ->
      addView(
        createFieldRow(field, technical).apply {
          if (index > 0) setPadding(0, if (technical) 6.dp else 4.dp, 0, 0)
        }
      )
    }
  }

  private fun createFieldRow(field: LibraryInsightField, technical: Boolean): View {
    val row = LinearLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      orientation = LinearLayout.HORIZONTAL
      gravity = if (technical) Gravity.TOP else Gravity.CENTER_VERTICAL
    }
    val label = AppCompatTextView(context).apply {
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, LABEL_WEIGHT).also {
        it.marginEnd = 8.dp
      }
      ellipsize = TextUtils.TruncateAt.END
      includeFontPadding = false
      isSingleLine = true
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      text = field.label
    }
    val value = AppCompatTextView(
      ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)
    ).apply {
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, VALUE_WEIGHT)
      gravity = Gravity.END
      includeFontPadding = false
      textAlignment = View.TEXT_ALIGNMENT_VIEW_END
      if (technical) {
        typeface = Typeface.MONOSPACE
        breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
        hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      } else {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      }
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

  private companion object {
    const val EXPANSION_DURATION = 350L
    const val LABEL_WEIGHT = 0.38f
    const val VALUE_WEIGHT = 0.62f
    val COMPACT_MIN_WIDTH = 160.dp
    val EXPANDABLE_MIN_WIDTH = 240.dp
  }
}
