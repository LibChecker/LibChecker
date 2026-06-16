package com.absinthe.libchecker.view.app

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.rulesbundle.IconResMap
import com.google.android.material.loadingindicator.LoadingIndicator
import kotlin.random.Random

class RuleLoadingView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

  private val loadingCarouselView = if (OsUtils.atLeastS()) {
    IconCarouselView(context).apply {
      layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT,
        Gravity.CENTER
      )
      setIconDrawables(createLoadingIconDrawables(context))
    }
  } else {
    null
  }

  init {
    clipChildren = false
    clipToPadding = false
    if (loadingCarouselView != null) {
      addView(loadingCarouselView)
    } else {
      addView(
        LoadingIndicator(
          ContextThemeWrapper(context, R.style.App_Widget_M3E_LoadingIndicator_Contained)
        ).apply {
          layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
          )
        }
      )
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    start()
  }

  override fun onDetachedFromWindow() {
    stop()
    super.onDetachedFromWindow()
  }

  fun setInitialIcon(@DrawableRes iconRes: Int, isSingleColorIcon: Boolean = false) {
    loadingCarouselView?.setIconDrawables(
      createLoadingIconDrawables(
        context = context,
        initialIcon = LoadingIcon(iconRes, isSingleColorIcon)
      )
    )
  }

  fun start() {
    loadingCarouselView?.start()
  }

  fun stop() {
    loadingCarouselView?.stop()
  }

  private fun createLoadingIconDrawables(
    context: Context,
    initialIcon: LoadingIcon? = null
  ): List<Drawable> {
    val randomIcons = (0 until LOADING_ICON_POOL_COUNT)
      .shuffled(Random(System.nanoTime()))
      .mapNotNull { index ->
        runCatching {
          LoadingIcon(
            iconRes = IconResMap.getIconRes(index),
            isSingleColorIcon = IconResMap.isSingleColorIcon(index)
          )
        }.getOrNull()
      }
      .filter { it.iconRes != initialIcon?.iconRes }
      .take(LOADING_ICON_COUNT - if (initialIcon == null) 0 else 1)

    return (listOfNotNull(initialIcon) + randomIcons)
      .mapNotNull {
        context.getCarouselIconDrawable(it.iconRes, it.isSingleColorIcon)
      }
      .ifEmpty {
        listOfNotNull(
          context.getCarouselIconDrawable(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android),
          context.getCarouselIconDrawable(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_google),
          context.getCarouselIconDrawable(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin),
          context.getCarouselIconDrawable(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_telegram)
        )
      }
  }

  private fun Context.getCarouselIconDrawable(
    @DrawableRes iconRes: Int,
    isSingleColorIcon: Boolean = runCatching {
      IconResMap.isSingleColorIcon(IconResMap.getResIndex(iconRes))
    }.getOrDefault(false)
  ): Drawable? {
    val drawable = ContextCompat.getDrawable(this, iconRes)?.mutate() ?: return null
    return DrawableCompat.wrap(drawable).mutate().also {
      if (isSingleColorIcon) {
        DrawableCompat.setTint(
          it,
          getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
        )
      }
    }
  }

  private companion object {
    private const val LOADING_ICON_COUNT = 18
    private const val LOADING_ICON_POOL_COUNT = 100
  }

  private data class LoadingIcon(
    @DrawableRes val iconRes: Int,
    val isSingleColorIcon: Boolean
  )
}
