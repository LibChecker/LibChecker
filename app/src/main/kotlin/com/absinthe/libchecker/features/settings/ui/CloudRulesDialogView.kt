package com.absinthe.libchecker.features.settings.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.libraries.utils.view.HeightAnimatableViewFlipper
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.button.MaterialButton

class CloudRulesDialogView(context: Context) :
  AViewGroup(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.cloud_rules)
  }

  private val viewFlipper = HeightAnimatableViewFlipper(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setInAnimation(context, R.anim.anim_fade_in)
    setOutAnimation(context, R.anim.anim_fade_out)
  }

  private val loadingAnim = LottieAnimationView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.lottie_anim_size)
    layoutParams = FrameLayout.LayoutParams(size, size).also {
      it.gravity = Gravity.CENTER
    }
    imageAssetsFolder = "/"
    repeatCount = LottieDrawable.INFINITE
    setAnimation("anim/gray-down-arrow.json")
  }

  val cloudRulesContentView = CloudRulesContentView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  init {
    addView(header)
    addView(viewFlipper)
    viewFlipper.addView(loadingAnim)
    viewFlipper.addView(cloudRulesContentView)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.autoMeasure()
    viewFlipper.autoMeasure()
    setMeasuredDimension(measuredWidth, header.measuredHeight + viewFlipper.measuredHeight)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    header.layout(0, paddingTop)
    viewFlipper.layout(0, header.bottom)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    loadingAnim.playAnimation()
  }

  class CloudRulesContentView(context: Context) : AViewGroup(context) {

    val localVersion = CloudRulesVersionView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      desc.text = context.getString(R.string.rules_local_repo_version)
    }

    private val arrow = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(48.dp, 48.dp)
      setImageResource(R.drawable.ic_arrow_right)
    }

    val remoteVersion = CloudRulesVersionView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      desc.text = context.getString(R.string.rules_remote_repo_version)
    }

    val updateButton = MaterialButton(context).apply {
      layoutParams = LayoutParams(300.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
        it.topMargin = 48.dp
      }
      isEnabled = false
      text = context.getString(R.string.rules_btn_update)
    }

    init {
      setPadding(0, 48.dp, 0, 48.dp)
      clipToPadding = false
      addView(localVersion)
      addView(arrow)
      addView(remoteVersion)
      addView(updateButton)
    }

    fun setUpdateButtonStatus(isEnable: Boolean) {
      if (isEnable) {
        updateButton.apply {
          isEnabled = true
          text = context.getString(R.string.rules_btn_restart_to_update)
        }
      } else {
        updateButton.apply {
          isEnabled = false
          text = context.getString(R.string.rules_btn_update)
        }
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      localVersion.autoMeasure()
      arrow.autoMeasure()
      remoteVersion.autoMeasure()
      updateButton.autoMeasure()
      setMeasuredDimension(
        measuredWidth,
        paddingTop + localVersion.measuredHeight + updateButton.marginTop + updateButton.measuredHeight + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      arrow.layout(arrow.toHorizontalCenter(this), paddingTop)
      localVersion.layout(
        arrow.left - 24.dp - localVersion.measuredWidth,
        localVersion.toViewVerticalCenter(arrow)
      )
      remoteVersion.layout(arrow.right + 24.dp, remoteVersion.toViewVerticalCenter(arrow))
      updateButton.layout(
        updateButton.toHorizontalCenter(this),
        localVersion.bottom + updateButton.marginTop
      )
    }
  }

  class CloudRulesVersionView(context: Context) : AViewGroup(context) {

    val version = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadline3))
    }

    val desc = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensedMedium
      )
    ).apply {
      layoutParams = LayoutParams(120.dp, ViewGroup.LayoutParams.WRAP_CONTENT)
      gravity = Gravity.CENTER
    }

    init {
      addView(version)
      addView(desc)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      version.autoMeasure()
      desc.autoMeasure()
      setMeasuredDimension(
        version.measuredWidth.coerceAtLeast(desc.measuredWidth),
        version.measuredHeight + desc.measuredHeight
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      version.layout(version.toHorizontalCenter(this), 0)
      desc.layout(desc.toHorizontalCenter(this), version.bottom)
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  fun showContent() {
    if (viewFlipper.displayedChildView != cloudRulesContentView) {
      viewFlipper.show(cloudRulesContentView)
    }
  }
}
