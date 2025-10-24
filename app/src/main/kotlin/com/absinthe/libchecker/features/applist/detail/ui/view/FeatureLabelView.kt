package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.bean.FeatureItem
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.animatedBlurAction
import com.absinthe.libchecker.utils.extensions.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FeatureLabelView(context: Context) : AppCompatImageButton(context) {

  private var drawables: List<Drawable>? = null
  private var currentIndex = 0
  private var iconSwitchJob: Job? = null
  private val coroutineScope = CoroutineScope(Dispatchers.Main)
  private val innerIconSize = 24.dp

  init {
    layoutParams = ViewGroup.MarginLayoutParams(36.dp, 36.dp)
    setPadding(6.dp, 6.dp, 6.dp, 6.dp)
    setBackgroundResource(R.drawable.ripple_feature_label_36dp)
    scaleType = ScaleType.CENTER_CROP
    clipToOutline = false
  }

  fun setFeature(item: FeatureItem) {
    item.colorFilterInt?.let {
      val drawable = UiUtils.changeDrawableColor(context, item.res, it)
      setImageDrawable(drawable)
    } ?: run {
      if (item.res != -1) {
        setImageResource(item.res)
      } else if (item.drawables != null) {
        initDrawables(item.drawables)
      }
    }
    setOnClickListener {
      item.action()
    }
  }

  private fun initDrawables(drawables: List<Drawable>?) {
    this.drawables = drawables?.map { drawable ->
      drawable.mutate().apply {
        setBounds(0, 0, innerIconSize, innerIconSize)
      }
    }
    startIconSwitchTask()
  }

  private fun startIconSwitchTask() {
    stopIconSwitchTask()
    val drawableList = drawables ?: return
    if (drawableList.isEmpty()) return

    setImageDrawable(drawableList[0])
    if (drawableList.size <= 1) return

    iconSwitchJob = coroutineScope.launch {
      while (isActive) {
        animatedBlurAction {
          setImageDrawable(drawableList[currentIndex])
        }

        currentIndex = (currentIndex + 1) % drawableList.size
        delay(3000)
      }
    }
  }

  private fun stopIconSwitchTask() {
    iconSwitchJob?.cancel()
    iconSwitchJob = null
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (drawables != null) {
      (parent as? ViewGroup)?.let {
        it.clipChildren = false
        it.clipToPadding = false
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopIconSwitchTask()
  }
}
