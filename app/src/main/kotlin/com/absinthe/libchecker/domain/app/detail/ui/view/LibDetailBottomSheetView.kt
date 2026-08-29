package com.absinthe.libchecker.domain.app.detail.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.method.LinkMovementMethod
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TableLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.ColorUtils
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.core.view.marginTop
import androidx.core.widget.TextViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailContentDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailHeaderDisplay
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.ui.adapter.addSpacingDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.absinthe.libchecker.view.app.RuleLoadingView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.HeightAnimatableViewFlipper
import com.google.android.material.tabs.TabLayout
import kotlin.math.roundToInt

class LibDetailBottomSheetView(
  context: Context,
  private val onLocaleSelected: (String) -> Unit = {},
  onInsightExpansionAnimationStateChange: (Boolean) -> Unit = {}
) : BottomSheetScaffoldView(context) {

  private val icon = AppCompatImageView(context).apply {
    val iconSize = 48.dp
    layoutParams = ViewGroup.MarginLayoutParams(iconSize, iconSize)
    setBackgroundResource(R.drawable.bg_circle_outline)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  private val title = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams = ViewGroup.MarginLayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 4.dp
    }
    gravity = Gravity.CENTER
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  private val extraInfoCard = LibraryExtraInfoCardView(
    context,
    onInsightExpansionAnimationStateChange
  )

  private val identityAndExtra = IdentityAndExtraView(
    context = context,
    icon = icon,
    title = title,
    extraInfoCard = extraInfoCard
  ).apply {
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
  }

  private val viewFlipper = HeightAnimatableViewFlipper(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    clipChildren = false
    clipToPadding = false
    setInAnimation(context, R.anim.anim_fade_in)
    setOutAnimation(context, R.anim.anim_fade_out)
  }

  private val loading = RuleLoadingView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      160.dp
    ).also {
      it.gravity = Gravity.CENTER
    }
  }

  private val notFoundView = NotFoundView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    ).also {
      it.gravity = Gravity.CENTER
    }
  }

  private var content: LibraryDetailContentDisplay? = null
  private var suppressLocaleSelected = false

  private val tabLayout = TabLayout(context).apply {
    layoutParams = TableLayout.LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    tabMode = TabLayout.MODE_SCROLLABLE
    setBackgroundColor(Color.TRANSPARENT)
    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab?) {
        tab?.let { bindLocale(it.position, notifySelection = !suppressLocaleSelected) }
      }

      override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

      override fun onTabReselected(tab: TabLayout.Tab?) = Unit
    })
  }

  private val contentAdapter = BindOnlyAdapter(::DetailInfoItemView, DetailInfoItemView::bind).apply {
    addHeaderView(tabLayout)
  }

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
    addSpacingDecoration(4.dp)
  }

  init {
    gravity = Gravity.CENTER_HORIZONTAL
    clipChildren = false
    clipToPadding = false
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    header.title.text = context.getString(R.string.lib_detail_dialog_title)
    addView(identityAndExtra)
    addView(viewFlipper)
    viewFlipper.addView(loading)
    viewFlipper.addView(contentView)
    viewFlipper.addView(notFoundView)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (viewFlipper.displayedChildView == loading) {
      loading.start()
    }
  }

  fun bind(state: LibraryDetailBottomSheetState) {
    title.text = state.title
    val headerDisplay = when (state) {
      is LibraryDetailBottomSheetState.Loading -> state.header
      is LibraryDetailBottomSheetState.Content -> state.header
      is LibraryDetailBottomSheetState.NotFound -> state.header
    }
    bindHeader(headerDisplay)

    when (state) {
      is LibraryDetailBottomSheetState.Loading -> showLoading()
      is LibraryDetailBottomSheetState.Content -> showContent(state.content)
      is LibraryDetailBottomSheetState.NotFound -> showNotFound()
    }
  }

  private fun bindHeader(headerDisplay: LibraryDetailHeaderDisplay?) {
    val iconRes = headerDisplay?.iconRes
      ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder
    loading.setInitialIcon(iconRes, headerDisplay?.isSimpleColorIcon == true)
    icon.load(iconRes) {
      crossfade(true)
      placeholder(R.drawable.ic_logo)
    }
  }

  fun renderLibraryInsight(state: LibraryInsightUiState, onRetry: () -> Unit) {
    val displayState = state.toDeferredDisplayState()
    val willBeGone = displayState == LibraryInsightUiState.Hidden
    val shouldAnimate = displayState.shouldAnimateReveal(
      isCurrentlyGone = extraInfoCard.isGone,
      isContainerLaidOut = identityAndExtra.isLaidOut
    )
    if (shouldAnimate) {
      TransitionManager.beginDelayedTransition(
        this,
        TransitionSet().apply {
          ordering = TransitionSet.ORDERING_TOGETHER
          duration = INSIGHT_VISIBILITY_DURATION
          interpolator = FastOutSlowInInterpolator()
          addTransition(
            ChangeBounds().apply {
              addTarget(icon)
              addTarget(title)
              addTarget(extraInfoCard)
              addTarget(identityAndExtra)
              addTarget(viewFlipper)
            }
          )
          addTransition(
            Fade(if (willBeGone) Fade.OUT else Fade.IN).apply {
              addTarget(extraInfoCard)
            }
          )
        }
      )
    }
    extraInfoCard.render(displayState, onRetry)
    identityAndExtra.setSurfaceVisible(!willBeGone, shouldAnimate)
  }

  private class IdentityAndExtraView(
    context: Context,
    private val icon: View,
    private val title: View,
    private val extraInfoCard: View
  ) : AViewGroup(context) {

    private val sectionGap = 16.dp
    private val surfaceInset = 12.dp
    private val surfaceColor =
      context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh)
    private val surfaceStrokeColor =
      context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
    private val surfaceDrawable = GradientDrawable().apply {
      cornerRadius = 12.dp.toFloat()
    }
    private var surfaceVisible = false
    private var surfaceProgress = 0f
    private var surfaceAnimator: ValueAnimator? = null
    private var identityWidth = 0
    private var contentHeight = 0

    init {
      background = surfaceDrawable
      applySurfaceProgress(0f)
      addView(icon)
      addView(title)
      addView(extraInfoCard)
    }

    fun setSurfaceVisible(visible: Boolean, animate: Boolean) {
      val targetProgress = if (visible) 1f else 0f
      if (surfaceVisible == visible && surfaceProgress == targetProgress) return
      surfaceVisible = visible
      requestLayout()
      surfaceAnimator?.cancel()
      if (!animate) {
        applySurfaceProgress(targetProgress)
        return
      }
      surfaceAnimator = ValueAnimator.ofFloat(surfaceProgress, targetProgress).apply {
        duration = INSIGHT_VISIBILITY_DURATION
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener { applySurfaceProgress(it.animatedValue as Float) }
        start()
      }
    }

    private fun applySurfaceProgress(progress: Float) {
      surfaceProgress = progress
      surfaceDrawable.setColor(
        ColorUtils.setAlphaComponent(
          surfaceColor,
          (Color.alpha(surfaceColor) * progress).roundToInt()
        )
      )
      surfaceDrawable.setStroke(
        1.dp,
        ColorUtils.setAlphaComponent(
          surfaceStrokeColor,
          (Color.alpha(surfaceStrokeColor) * progress).roundToInt()
        )
      )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      val widthLimit = MeasureSpec.getSize(widthMeasureSpec)
      val horizontalInset = if (surfaceVisible) surfaceInset else 0
      val verticalInset = if (surfaceVisible) surfaceInset else 0
      val availableWidth = (
        widthLimit -
          paddingStart -
          paddingEnd -
          horizontalInset * 2
        ).coerceAtLeast(0)
      icon.measure(icon.layoutParams.width.toExactlyMeasureSpec(), icon.layoutParams.height.toExactlyMeasureSpec())
      title.measure(availableWidth.toAtMostMeasureSpec(), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
      val identityOnlyWidth = maxOf(icon.measuredWidth, title.measuredWidth)
      identityWidth = if (surfaceVisible) availableWidth else identityOnlyWidth
      title.measure(identityWidth.toAtMostMeasureSpec(), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
      val identityHeight = icon.measuredHeight + 4.dp + title.measuredHeight

      contentHeight = when {
        extraInfoCard.isGone -> identityHeight

        else -> {
          extraInfoCard.measure(availableWidth.toExactlyMeasureSpec(), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
          identityHeight + sectionGap + extraInfoCard.measuredHeight
        }
      }
      val desiredWidth = if (surfaceVisible) {
        widthLimit
      } else {
        identityWidth + paddingStart + paddingEnd
      }
      val desiredHeight =
        contentHeight + paddingTop + paddingBottom + verticalInset * 2
      setMeasuredDimension(
        resolveSize(desiredWidth, widthMeasureSpec),
        resolveSize(desiredHeight, heightMeasureSpec)
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      val identityHeight = icon.measuredHeight + 4.dp + title.measuredHeight
      val horizontalInset = if (surfaceVisible) surfaceInset else 0
      val verticalInset = if (surfaceVisible) surfaceInset else 0
      val identityTop = paddingTop + verticalInset
      val identityLeft = paddingStart + horizontalInset
      val iconX = identityLeft + (identityWidth - icon.measuredWidth) / 2
      icon.layout(iconX, identityTop)
      title.layout(
        identityLeft + (identityWidth - title.measuredWidth) / 2,
        icon.bottom + 4.dp
      )
      if (extraInfoCard.isGone) return
      val cardLeft = paddingStart + horizontalInset
      val cardTop = paddingTop + verticalInset + identityHeight + sectionGap
      extraInfoCard.layout(cardLeft, cardTop)
    }
  }

  private fun showLoading() {
    if (viewFlipper.displayedChildView != loading) {
      viewFlipper.show(loading)
    }
    if (isAttachedToWindow) {
      loading.start()
    }
  }

  private fun showContent(content: LibraryDetailContentDisplay) {
    this.content = content
    suppressLocaleSelected = true
    tabLayout.removeAllTabs()
    content.locales.forEach { locale ->
      tabLayout.addTab(tabLayout.newTab().setText(locale.localeName), false)
    }
    val selectedPosition = content.locales.indexOfFirst {
      it.localeTag == content.selectedLocaleTag
    }
    tabLayout.selectTab(tabLayout.getTabAt(selectedPosition))
    suppressLocaleSelected = false
    bindLocale(selectedPosition, notifySelection = false)

    loading.stop()
    if (viewFlipper.displayedChildView != contentView) {
      viewFlipper.show(contentView)
    }
  }

  private fun bindLocale(position: Int, notifySelection: Boolean) {
    val locale = content?.locales?.getOrNull(position) ?: return
    contentAdapter.setList(locale.items)
    if (notifySelection) {
      onLocaleSelected(locale.localeTag)
    }
  }

  private fun showNotFound() {
    loading.stop()
    if (viewFlipper.displayedChildView != notFoundView) {
      viewFlipper.show(notFoundView)
    }
  }

  private companion object {
    const val INSIGHT_VISIBILITY_DURATION = 350L
  }

  private class NotFoundView(context: Context) : AViewGroup(context) {

    private val icon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(64.dp, 64.dp)
      setImageResource(R.drawable.ic_failed)
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val notFoundText = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      text = context.getString(R.string.not_found)
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodyMedium))
    }

    private val createNewIssueText = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      text = context.getString(R.string.create_an_issue)
      setLinkTextColor(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
      gravity = Gravity.CENTER_VERTICAL
      setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_github, 0, 0, 0)
      compoundDrawablePadding = 4.dp
      TextViewCompat.setCompoundDrawableTintList(
        this,
        ColorStateList.valueOf(context.getColorByAttr(android.R.attr.colorControlNormal))
      )
      isClickable = true
      movementMethod = LinkMovementMethod.getInstance()
      text = HtmlCompat.fromHtml(
        "<a href='${ApiManager.GITHUB_NEW_ISSUE_URL}'> ${resources.getText(R.string.create_an_issue)} </a>",
        HtmlCompat.FROM_HTML_MODE_LEGACY
      )
    }

    init {
      addView(icon)
      addView(notFoundText)
      addView(createNewIssueText)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      icon.autoMeasure()
      notFoundText.autoMeasure()
      createNewIssueText.autoMeasure()
      setMeasuredDimension(
        measuredWidth,
        icon.measuredHeight + notFoundText.measuredHeight + createNewIssueText.marginTop + createNewIssueText.measuredHeight
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(icon.toHorizontalCenter(this), 0)
      notFoundText.layout(notFoundText.toHorizontalCenter(this), icon.bottom)
      createNewIssueText.layout(
        createNewIssueText.toHorizontalCenter(this),
        notFoundText.bottom + createNewIssueText.marginTop
      )
    }
  }
}

internal fun LibraryInsightUiState.toDeferredDisplayState(): LibraryInsightUiState {
  return if (this == LibraryInsightUiState.Loading) {
    LibraryInsightUiState.Hidden
  } else {
    this
  }
}

internal fun LibraryInsightUiState.shouldAnimateReveal(
  isCurrentlyGone: Boolean,
  isContainerLaidOut: Boolean
): Boolean {
  return this is LibraryInsightUiState.Content && isCurrentlyGone && isContainerLaidOut
}
