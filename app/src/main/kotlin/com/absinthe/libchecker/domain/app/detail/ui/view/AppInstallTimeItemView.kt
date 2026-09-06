package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.action.AppInstalledTimeDisplayData
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.view.AViewGroup

class AppInstallTimeItemView(context: Context) : AppInstallDetailSectionView(context, R.string.lib_detail_app_installed_time) {

  private val contentView = ContentView(context).also(::setContentView)

  fun bind(display: AppInstalledTimeDisplayData) {
    contentView.bind(
      firstInstalledTime = display.firstInstalledTime,
      lastUpdatedTime = display.lastUpdatedTime
    )
    container.setLongClickCopiedToClipboard(contentView.getAllContentText())
  }

  private class ContentView(context: Context) : AViewGroup(context) {
    private val firstInstalledLabel = context.getString(R.string.lib_detail_app_first_installed_time)
    private val lastUpdatedLabel = context.getString(R.string.lib_detail_app_last_updated_time)
    private var firstInstalledTime: CharSequence = ""
    private var lastUpdatedTime: CharSequence = ""

    private val firstInstalledView = NativeLibItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      isClickable = false
      isLongClickable = false
      bindText(firstInstalledLabel, firstInstalledTime)
    }

    private val lastUpdatedView = NativeLibItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      isClickable = false
      isLongClickable = false
      bindText(lastUpdatedLabel, lastUpdatedTime)
    }

    init {
      setPadding(0, 8.dp, 0, 8.dp)
      addView(firstInstalledView)
      addView(lastUpdatedView)
    }

    fun bind(
      firstInstalledTime: CharSequence,
      lastUpdatedTime: CharSequence
    ) {
      this.firstInstalledTime = firstInstalledTime
      this.lastUpdatedTime = lastUpdatedTime
      firstInstalledView.bindText(firstInstalledLabel, firstInstalledTime)
      lastUpdatedView.bindText(lastUpdatedLabel, lastUpdatedTime)
    }

    fun getAllContentText(): String {
      return listOf(
        firstInstalledLabel,
        firstInstalledTime,
        lastUpdatedLabel,
        lastUpdatedTime
      ).joinToString(System.lineSeparator())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      firstInstalledView.autoMeasure()
      lastUpdatedView.autoMeasure()
      setMeasuredDimension(
        measuredWidth,
        paddingTop +
          firstInstalledView.marginTop +
          firstInstalledView.measuredHeight +
          lastUpdatedView.marginTop +
          lastUpdatedView.measuredHeight +
          lastUpdatedView.marginBottom +
          paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      firstInstalledView.layout(paddingStart, firstInstalledView.marginTop + paddingTop)
      lastUpdatedView.layout(paddingStart, firstInstalledView.bottom + lastUpdatedView.marginTop)
    }
  }
}
