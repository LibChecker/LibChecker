package com.absinthe.libchecker.domain.app.detail.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.FeatureItem
import com.absinthe.libchecker.utils.OsUtils
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

  private var drawables: List<Drawable> = emptyList()
  private var currentIndex = 0
  private var iconSwitchJob: Job? = null
  private var iconSwitchAnimator: ValueAnimator? = null
  private val coroutineScope = CoroutineScope(Dispatchers.Main)
  private val innerIconSize = 24.dp

  init {
    layoutParams = ViewGroup.MarginLayoutParams(36.dp, 36.dp)
    setPadding(6.dp, 6.dp, 6.dp, 6.dp)
    setBackgroundResource(R.drawable.ripple_feature_label_36dp)
    scaleType = ScaleType.CENTER_CROP
    clipToOutline = false
  }

  fun bind(item: FeatureItem, onClick: () -> Unit) {
    recycle()
    contentDescription = item.titleRes.takeIf { it != 0 }?.let(context::getString)
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
    setOnClickListener { onClick() }
  }

  fun recycle() {
    stopIconSwitchTask()
    drawables = emptyList()
    currentIndex = 0
    contentDescription = null
    setImageDrawable(null)
    setOnClickListener(null)
  }

  private fun initDrawables(drawables: List<Drawable>?) {
    this.drawables = drawables?.map { drawable ->
      drawable.mutate().apply {
        setBounds(0, 0, innerIconSize, innerIconSize)
      }
    }.orEmpty()
    currentIndex = 0
    this.drawables.firstOrNull()?.let(::setImageDrawable)
    if (isAttachedToWindow) {
      startIconSwitchTask()
    }
  }

  private fun startIconSwitchTask() {
    stopIconSwitchTask()
    val drawableList = drawables
    if (drawableList.isEmpty()) return

    setImageDrawable(drawableList[0])
    if (drawableList.size <= 1) return

    iconSwitchJob = coroutineScope.launch {
      while (isActive) {
        currentIndex = (currentIndex + 1) % drawableList.size
        iconSwitchAnimator = animatedBlurAction {
          setImageDrawable(drawableList[currentIndex])
        }
        delay(3000)
      }
    }
  }

  private fun stopIconSwitchTask() {
    iconSwitchJob?.cancel()
    iconSwitchJob = null
    iconSwitchAnimator?.cancel()
    iconSwitchAnimator = null
    if (OsUtils.atLeastS()) {
      setRenderEffect(null)
    }
    alpha = 1f
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (drawables.isNotEmpty()) {
      (parent as? ViewGroup)?.let {
        it.clipChildren = false
        it.clipToPadding = false
      }
      startIconSwitchTask()
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopIconSwitchTask()
  }
}
