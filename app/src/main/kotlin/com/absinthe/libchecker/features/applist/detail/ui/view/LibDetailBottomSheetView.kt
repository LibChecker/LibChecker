package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TableLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.core.view.children
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibDetailItemAdapter
import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.LibDetailItem
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.libraries.utils.view.HeightAnimatableViewFlipper
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.tabs.TabLayout
import java.util.Locale
import timber.log.Timber

class LibDetailBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.lib_detail_dialog_title)
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
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 4.dp
    }
    gravity = Gravity.CENTER
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  private val viewFlipper = HeightAnimatableViewFlipper(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setInAnimation(context, R.anim.anim_fade_in)
    setOutAnimation(context, R.anim.anim_fade_out)
  }

  private val loading = LottieAnimationView(context).apply {
    layoutParams = FrameLayout.LayoutParams(200.dp, 200.dp).also {
      it.gravity = Gravity.CENTER
    }
    imageAssetsFolder = "/"
    repeatCount = LottieDrawable.INFINITE
    setAnimation("anim/lib_detail_rocket.json")
  }

  private val notFoundView = NotFoundView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    ).also {
      it.gravity = Gravity.CENTER
    }
  }

  private val contentAdapter = LibDetailItemAdapter().apply {
    addHeaderView(
      TabLayout(context).apply {
        layoutParams = TableLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setBackgroundColor(Color.TRANSPARENT)
        tabMode = TabLayout.MODE_SCROLLABLE
        // TODO: Replace with real data
        addTab(newTab().setText(Locale("zh").displayLanguage))
      }
    )
  }

  private val libDetailContentView = BottomSheetRecyclerView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    )
    adapter = contentAdapter
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
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
    addView(viewFlipper)
    viewFlipper.addView(loading)
    viewFlipper.addView(libDetailContentView)
    viewFlipper.addView(notFoundView)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    loading.playAnimation()
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  class LibDetailItemView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(24.dp, 24.dp)
    }

    val tip = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      alpha = 0.65f
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
    }

    val text = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
        it.topMargin = 0
      }
    }

    init {
      setPadding(8.dp, 8.dp, 8.dp, 8.dp)
      setBackgroundResource(R.drawable.bg_lib_detail_item)
      addView(icon)
      addView(tip)
      addView(text)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val textWidth = measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - tip.marginStart
      if (tip.measuredWidth > textWidth) {
        tip.measure(textWidth.toExactlyMeasureSpec(), tip.defaultHeightMeasureSpec(this))
      }
      if (text.measuredWidth > textWidth) {
        text.measure(textWidth.toExactlyMeasureSpec(), text.defaultHeightMeasureSpec(this))
      }
      setMeasuredDimension(
        measuredWidth,
        (tip.measuredHeight + text.marginTop + text.measuredHeight).coerceAtLeast(icon.measuredHeight) + paddingTop + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      tip.layout(paddingStart + icon.measuredWidth + tip.marginStart, paddingTop)
      text.layout(paddingStart + icon.measuredWidth + tip.marginStart, tip.bottom + text.marginTop)
    }
  }

  class NotFoundView(context: Context) : AViewGroup(context) {

    private val icon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(64.dp, 64.dp)
      setImageResource(R.drawable.ic_failed)
    }

    private val notFoundText = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      text = context.getString(R.string.not_found)
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2))
    }

    private val createNewIssueText = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      text = context.getString(R.string.create_an_issue)
      setLinkTextColor(R.color.colorPrimary.getColor(context))
      gravity = Gravity.CENTER_VERTICAL
      setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_github, 0, 0, 0)
      compoundDrawablePadding = 4.dp
      TextViewCompat.setCompoundDrawableTintList(
        this,
        ColorStateList.valueOf(
          context.getColorByAttr(android.R.attr.colorControlNormal)
        )
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

  fun setContent(label: String, devTeam: String, ruleContributors: String, description: String, sourceLink: String) {
    val list = listOf(
      LibDetailItem(
        iconRes = R.drawable.ic_label,
        tipRes = R.string.lib_detail_label_tip,
        textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2),
        text = label
      ),
      LibDetailItem(
        iconRes = R.drawable.ic_team,
        tipRes = R.string.lib_detail_develop_team_tip,
        textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2),
        text = devTeam
      ),
      LibDetailItem(
        iconRes = R.drawable.ic_github,
        tipRes = R.string.lib_detail_rule_contributors_tip,
        textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2),
        text = ruleContributors
      ),
      LibDetailItem(
        iconRes = R.drawable.ic_content,
        tipRes = R.string.lib_detail_description_tip,
        textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2),
        text = description
      ),
      LibDetailItem(
        iconRes = R.drawable.ic_url,
        tipRes = R.string.lib_detail_relative_link_tip,
        textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2),
        text = sourceLink
      )
    )
    contentAdapter.setList(list)
  }

  fun setUpdateTIme(time: String) {
    contentAdapter.addData(
      LibDetailItem(
        iconRes = R.drawable.ic_time,
        tipRes = R.string.lib_detail_last_update_tip,
        textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2),
        text = time
      )
    )
  }

  fun showContent() {
    Timber.d("showContent")
    if (viewFlipper.displayedChildView != libDetailContentView) {
      viewFlipper.show(libDetailContentView)
    }
  }

  fun showNotFound() {
    if (viewFlipper.displayedChildView != notFoundView) {
      viewFlipper.show(notFoundView)
    }
  }
}
