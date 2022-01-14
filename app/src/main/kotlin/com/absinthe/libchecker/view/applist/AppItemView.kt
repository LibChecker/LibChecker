package com.absinthe.libchecker.view.applist

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class AppItemView(context: Context) : MaterialCardView(context) {

  val container = AppItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
  }

  private val floatView by lazy {
    AppCompatTextView(
      ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.gravity = Gravity.CENTER
        it.topMargin = 24.dp
        it.bottomMargin = 24.dp
      }
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    }
  }

  init {
    addView(container)
  }

  fun addFloatView(text: String) {
    if (container.parent != null) {
      removeView(container)
    }
    if (floatView.parent == null) {
      addView(floatView)
    }
    floatView.text = text
  }

  class AppItemContainerView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageView(context).apply {
      val iconSize = context.getDimensionPixelSize(R.dimen.app_icon_size)
      layoutParams = LayoutParams(iconSize, iconSize)
      addView(this)
    }

    val appName = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifMedium
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      setTextColor(context.getColorByAttr(R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      addView(this)
    }

    val packageName =
      AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextColor(context.getColorByAttr(R.attr.colorOnSurface))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        addView(this)
      }

    val versionInfo = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensed
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      addView(this)
    }

    val abiInfo = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensedMedium
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(android.R.color.darker_gray.getColor(context))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      addView(this)
    }

    private var badge: AppCompatImageView? = null

    fun setBadge(res: Int) {
      setBadge(res.getDrawable(context))
    }

    fun setBadge(drawable: Drawable?) {
      if (drawable != null) {
        if (badge == null) {
          badge = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(24.dp, 24.dp)
            addView(this)
          }
        }
        badge!!.setImageDrawable(drawable)
      } else {
        if (badge != null) {
          removeView(badge)
          badge = null
        }
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      icon.autoMeasure()
      val textWidth =
        measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - appName.marginStart
      appName.measure(
        textWidth.toExactlyMeasureSpec(),
        appName.defaultHeightMeasureSpec(this)
      )
      packageName.measure(
        textWidth.toExactlyMeasureSpec(),
        packageName.defaultHeightMeasureSpec(this)
      )
      versionInfo.measure(
        textWidth.toExactlyMeasureSpec(),
        versionInfo.defaultHeightMeasureSpec(this)
      )
      abiInfo.measure(
        textWidth.toExactlyMeasureSpec(),
        abiInfo.defaultHeightMeasureSpec(this)
      )
      badge?.autoMeasure()
      setMeasuredDimension(
        measuredWidth,
        paddingTop + appName.measuredHeight + packageName.measuredHeight + versionInfo.measuredHeight + abiInfo.measuredHeight + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      appName.layout(icon.right + appName.marginStart, paddingTop)
      packageName.layout(appName.left, appName.bottom)
      versionInfo.layout(appName.left, packageName.bottom)
      abiInfo.layout(appName.left, versionInfo.bottom)
      badge?.layout(paddingTop, paddingEnd, fromRight = true)
    }
  }
}
