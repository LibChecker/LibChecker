package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.R as MaterialR
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.SuperEllipseCornerTreatment

class TimeNodeItemView(context: Context) : AViewGroup(context) {

  private val defaultNameColor: Int
  private var packageIconSources: Map<String, SnapshotPackageIconSource> = emptyMap()

  private val itemBackground = MaterialShapeDrawable(
    ShapeAppearanceModel.builder()
      .setAllCorners(SuperEllipseCornerTreatment(16.dp.toFloat()))
      .build()
  ).apply {
    strokeWidth = 1.dp.toFloat()
    strokeColor = ColorStateList.valueOf(context.getColorByAttr(MaterialR.attr.colorOutlineVariant))
    fillColor = ColorStateList.valueOf(Color.TRANSPARENT)
  }

  private val tagIndicator = View(context).apply {
    layoutParams = LayoutParams(8.dp, 8.dp)
  }

  private val name = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }

  private val summary = AppCompatTextView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextColor(context.getColorByAttr(android.R.attr.textColorSecondary))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
  }

  private val checkIndicator = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(20.dp, 20.dp)
    setImageResource(R.drawable.ic_check)
    setColorFilter(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
    isVisible = false
  }

  private val adapter = BindOnlyAdapter<String, AppCompatImageView>(
    viewFactory = {
      AppCompatImageView(it).apply {
        layoutParams = ViewGroup.LayoutParams(20.dp, 20.dp)
        setPadding(0, 0, 3.dp, 0)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
      }
    },
    bindView = { packageName ->
      when (val iconSource = packageIconSources[packageName]) {
        is SnapshotPackageIconSource.InstalledPackage -> load(iconSource.packageInfo)

        SnapshotPackageIconSource.Fallback,
        null -> load(R.drawable.ic_icon_blueprint)
      }
    }
  )

  private val moreIndicator = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      gravity = Gravity.CENTER_VERTICAL
    }
    // noinspection AndroidLintSetTextI18n
    text = "…"
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
  }

  private val rvList = RecyclerView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    overScrollMode = OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    adapter = this@TimeNodeItemView.adapter
  }

  init {
    defaultNameColor = name.currentTextColor
    setPadding(10.dp, 6.dp, 10.dp, 6.dp)
    background = itemBackground
    addView(tagIndicator)
    addView(name)
    addView(summary)
    addView(checkIndicator)
    addView(rvList)
  }

  fun bind(
    item: SnapshotTimeNodeItem,
    packageIconSources: Map<String, SnapshotPackageIconSource>
  ) {
    name.text = item.timestampText
    val appCountText = resources.getQuantityString(
      R.plurals.snapshot_time_node_apps_count,
      item.appCount,
      item.appCount
    )
    summary.text = appCountText

    val primaryColor = context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
    val isItemActive = item.isSelected
    name.setTextColor(if (isItemActive) primaryColor else defaultNameColor)
    summary.setTextColor(
      if (isItemActive) {
        primaryColor
      } else {
        context.getColorByAttr(android.R.attr.textColorSecondary)
      }
    )

    checkIndicator.isVisible = item.isSelected

    if (item.tagColor != null) {
      tagIndicator.isVisible = true
      tagIndicator.background = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(item.tagColor)
      }
    } else {
      tagIndicator.isVisible = false
    }

    val outlineVariant = context.getColorByAttr(MaterialR.attr.colorOutlineVariant)
    val surfaceHigh = context.getColorByAttr(MaterialR.attr.colorSurfaceContainerHigh)
    if (item.isSelected) {
      itemBackground.strokeWidth = 2.dp.toFloat()
      itemBackground.strokeColor = ColorStateList.valueOf(primaryColor)
      itemBackground.fillColor = ColorStateList.valueOf(surfaceHigh)
    } else {
      itemBackground.strokeWidth = 1.dp.toFloat()
      itemBackground.strokeColor = ColorStateList.valueOf(outlineVariant)
      itemBackground.fillColor = ColorStateList.valueOf(Color.TRANSPARENT)
    }

    contentDescription = listOf(item.description, summary.text)
      .map(CharSequence::toString)
      .filter(String::isNotBlank)
      .joinToString()
    this.packageIconSources = packageIconSources
    adapter.setList(item.topAppPackageNames.take(MAX_VISIBLE_APP_COUNT))
    rvList.isVisible = item.topAppPackageNames.isNotEmpty()
    if (item.topAppPackageNames.size <= MAX_VISIBLE_APP_COUNT) {
      adapter.removeAllFooterView()
    } else {
      adapter.setFooterView(moreIndicator)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (tagIndicator.isVisible) {
      tagIndicator.measure(8.dp.toExactlyMeasureSpec(), 8.dp.toExactlyMeasureSpec())
    }
    if (checkIndicator.isVisible) {
      checkIndicator.measure(20.dp.toExactlyMeasureSpec(), 20.dp.toExactlyMeasureSpec())
    }
    summary.autoMeasure()

    val tagWidth = if (tagIndicator.isVisible) tagIndicator.measuredWidth + TAG_GAP.dp else 0
    val availableNameWidth = (measuredWidth - paddingStart - paddingEnd - tagWidth - summary.measuredWidth - TITLE_GAP.dp)
      .coerceAtLeast(0)

    name.measure(
      availableNameWidth.toExactlyMeasureSpec(),
      name.defaultHeightMeasureSpec(this)
    )

    val startX = paddingStart + tagWidth
    val checkWidth = if (checkIndicator.isVisible) checkIndicator.measuredWidth + 8.dp else 0
    val availableRvWidth = (measuredWidth - startX - paddingEnd - checkWidth).coerceAtLeast(0)

    if (rvList.isVisible) {
      rvList.measure(
        availableRvWidth.toAtMostMeasureSpec(),
        rvList.defaultHeightMeasureSpec(this)
      )
    } else {
      rvList.measure(0.toExactlyMeasureSpec(), 0.toExactlyMeasureSpec())
    }

    val row2Height = maxOf(
      if (rvList.isVisible) rvList.measuredHeight else 0,
      if (checkIndicator.isVisible) checkIndicator.measuredHeight else 0
    )
    setMeasuredDimension(
      measuredWidth,
      maxOf(
        MIN_HEIGHT.dp,
        paddingTop + paddingBottom + name.measuredHeight + row2Height
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    var startX = paddingStart
    if (tagIndicator.isVisible) {
      tagIndicator.layout(startX, tagIndicator.toViewVerticalCenter(name))
      startX += tagIndicator.measuredWidth + TAG_GAP.dp
    }
    name.layout(startX, paddingTop)
    summary.layout(paddingEnd, summary.toViewVerticalCenter(name), fromRight = true)

    val row2Top = name.bottom
    if (rvList.isVisible) {
      rvList.layout(startX, row2Top)
    }
    if (checkIndicator.isVisible) {
      val checkTop = if (rvList.isVisible) {
        checkIndicator.toViewVerticalCenter(rvList)
      } else {
        row2Top
      }
      checkIndicator.layout(paddingEnd, checkTop, fromRight = true)
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    return true
  }

  private companion object {
    const val MAX_VISIBLE_APP_COUNT = 6
    const val MIN_HEIGHT = 44
    const val TITLE_GAP = 8
    const val TAG_GAP = 6
  }
}
