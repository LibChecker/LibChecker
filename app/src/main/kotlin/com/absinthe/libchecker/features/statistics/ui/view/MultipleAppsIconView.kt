package com.absinthe.libchecker.features.statistics.ui.view

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import coil.load
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.AViewGroup

class MultipleAppsIconView(context: Context) : AViewGroup(context) {

  private val icons = ArrayList<AppCompatImageView>(4)
  private val iconGap = 2.dp
  private var iconSize = 0f

  init {
    val outlineProvider: ViewOutlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(Rect(0, 0, view.width, view.width), view.width.toFloat())
      }
    }
    clipToOutline = true
    setOutlineProvider(outlineProvider)
    setBackgroundColor(context.getColorByAttr(com.google.android.material.R.attr.colorSecondaryContainer))
  }

  fun setIcons(packages: List<String>) {
    if (icons.isNotEmpty()) {
      removeAllViews()
      icons.clear()
    }
    while (icons.size < 4 && icons.size < packages.size) {
      val icon = AppCompatImageView(context).also {
        val packageName = packages[icons.size]
        val packageInfo = runCatching {
          PackageUtils.getPackageInfo(packageName)
        }.getOrNull() ?: return

        it.load(packageInfo)
      }
      icons.add(icon)
      addView(icon)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    iconSize = when (icons.size) {
      1 -> measuredWidth * 0.75f
      2, 3, 4 -> measuredWidth * 0.4f
      else -> 0f
    }
    icons.forEach {
      it.measure(iconSize.toInt().toExactlyMeasureSpec(), iconSize.toInt().toExactlyMeasureSpec())
    }
    setMeasuredDimension(measuredWidth, measuredHeight)
  }

  override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
    if (icons.isEmpty()) {
      return
    }
    when (icons.size) {
      1 -> {
        icons[0].also {
          it.layout(it.toHorizontalCenter(this), it.toVerticalCenter(this))
        }
      }

      2 -> {
        val paddingHorizontal = (measuredWidth - iconSize * 2 - iconGap).div(2)
        icons.forEachIndexed { index, view ->
          val x = paddingHorizontal + (iconSize + iconGap) * index
          val y = (measuredHeight - iconSize) * 0.5f
          view.layout(x.toInt(), y.toInt())
        }
      }

      3 -> {
        val padding = (measuredWidth - iconSize * 2 - iconGap).div(2)
        icons.forEachIndexed { index, view ->
          if (index == 0) {
            view.layout(view.toHorizontalCenter(this), padding.toInt())
          } else {
            val x = padding + (iconSize + iconGap) * (index - 1)
            val y = padding + iconSize + iconGap
            view.layout(x.toInt(), y.toInt())
          }
        }
      }

      4 -> {
        val padding = (measuredWidth - iconSize * 2 - iconGap).div(2)
        icons.forEachIndexed { index, view ->
          val x = padding + (iconSize + iconGap) * index.mod(2)
          val y = padding + (iconSize + iconGap) * index.div(2)
          view.layout(x.toInt(), y.toInt())
        }
      }
    }
  }
}
