package com.absinthe.libchecker.ui.base

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.core.animation.doOnEnd
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libraries.utils.R
import com.absinthe.libraries.utils.utils.UiUtils
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlin.math.roundToInt
import timber.log.Timber

abstract class BaseBottomSheetViewDialogFragment<T : View> :
  BottomSheetDialogFragment(),
  View.OnLayoutChangeListener {

  var animationDuration = 350L
  var maxPeekHeightPercentage = 0f
  var maxPeekSize: Int = 0

  private var _root: T? = null
  private var isHandlerActivated = false
  private var animator: ValueAnimator = ObjectAnimator()
  private val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  private val maxBlurRadius = 64
  private var maxDimAmount: Float = 0f
  private var dialogWindow: Window? = null
  private val behavior by lazy { BottomSheetBehavior.from(root.parent as View) }
  private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) {
      when (newState) {
        BottomSheetBehavior.STATE_DRAGGING -> {
          if (!isHandlerActivated) {
            isHandlerActivated = true
            getHeaderView().onHandlerActivated(true)
          }
        }

        BottomSheetBehavior.STATE_COLLAPSED -> {
          if (isHandlerActivated) {
            isHandlerActivated = false
            getHeaderView().onHandlerActivated(false)
          }
          updateBlurAndDimForOffset(0f)
        }

        BottomSheetBehavior.STATE_EXPANDED -> {
          if (isHandlerActivated) {
            isHandlerActivated = false
            getHeaderView().onHandlerActivated(false)
          }
          updateBlurAndDimForOffset(1f)
          bottomSheet.background = createMaterialShapeDrawable(bottomSheet)
        }

        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
          updateBlurAndDimForOffset(0.5f)
        }

        BottomSheetBehavior.STATE_HIDDEN -> {
          resetBlurRadius()
        }

        else -> {
        }
      }
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
      updateBlurAndDimForOffset(slideOffset)
    }
  }

  val root get() = _root!!

  abstract fun initRootView(): T
  abstract fun init()
  abstract fun getHeaderView(): BottomSheetHeaderView

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = object : BottomSheetDialog(requireContext(), theme) {
    override fun onAttachedToWindow() {
      super.onAttachedToWindow()

      window?.let {
        dialogWindow = it
        it.attributes?.windowAnimations = R.style.DialogAnimation
        WindowCompat.setDecorFitsSystemWindows(it, false)
        UiUtils.setSystemBarStyle(it)
        WindowInsetsControllerCompat(it, it.decorView)
          .isAppearanceLightNavigationBars = !UiUtils.isDarkMode()
        maxDimAmount = it.attributes.dimAmount

        if (supportsBlur) {
          it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
          it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
          updateBlurAndDimForOffset(1f)
        }
      }

      findViewById<View>(com.google.android.material.R.id.container)?.fitsSystemWindows =
        false
      findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows =
        false
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _root = initRootView().apply { applyRootView(this) }
    init()
    return _root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.viewTreeObserver.addOnGlobalLayoutListener(object :
      ViewTreeObserver.OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        activity?.window?.takeIf { isLandscape(it) }?.run {
          behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }
      }
    })
  }

  override fun onStart() {
    super.onStart()
    behavior.addBottomSheetCallback(bottomSheetCallback)
    root.addOnLayoutChangeListener(this)
  }

  override fun onStop() {
    super.onStop()
    behavior.removeBottomSheetCallback(bottomSheetCallback)
  }

  override fun onDetach() {
    animator.cancel()
    resetBlurRadius()
    dialogWindow = null
    super.onDetach()
  }

  override fun onDestroyView() {
    animator.cancel()
    root.removeOnLayoutChangeListener(this)
    resetBlurRadius()
    _root = null
    super.onDestroyView()
  }

  override fun show(manager: FragmentManager, tag: String?) {
    runCatching {
      super.show(manager, tag)
    }.onFailure {
      Timber.Forest.e(it.toString())
    }
  }

  override fun onLayoutChange(
    view: View,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    oldLeft: Int,
    oldTop: Int,
    oldRight: Int,
    oldBottom: Int
  ) {
    if ((bottom - top) != (oldBottom - oldTop)) {
      enqueueAnimation {
        animateHeight(from = oldBottom - oldTop, to = bottom - top, onEnd = { })
      }
    }
  }

  private fun applyRootView(root: T) {
    root.post {
      if (maxPeekHeightPercentage >= 0f) {
        maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * maxPeekHeightPercentage).toInt()
      } else {
        throw IllegalArgumentException("maxPeekHeightPercentage must be greater than 0")
      }
    }
  }

  private fun createMaterialShapeDrawable(bottomSheet: View): MaterialShapeDrawable {
    // Create a ShapeAppearanceModel with the same shapeAppearanceOverlay used in the style
    val shapeAppearanceModel =
      ShapeAppearanceModel.builder(context, 0, R.style.CustomShapeAppearanceBottomSheetDialog)
        .build()

    // Create a new MaterialShapeDrawable (you can't use the original MaterialShapeDrawable in the BottomSheet)
    val currentMaterialShapeDrawable = bottomSheet.background as MaterialShapeDrawable
    return MaterialShapeDrawable(shapeAppearanceModel).apply {
      // Copy the attributes in the new MaterialShapeDrawable
      initializeElevationOverlay(context)
      fillColor = currentMaterialShapeDrawable.fillColor
      tintList = currentMaterialShapeDrawable.tintList
      elevation = currentMaterialShapeDrawable.elevation
      strokeWidth = currentMaterialShapeDrawable.strokeWidth
      strokeColor = currentMaterialShapeDrawable.strokeColor
    }
  }

  private fun animateHeight(from: Int, to: Int, onEnd: () -> Unit) {
    animator.cancel()
    animator = ObjectAnimator.ofFloat(0f, 1f).apply {
      duration = animationDuration
      interpolator = FastOutSlowInInterpolator()
      Timber.Forest.d("animateHeight: $from -> $to")

      addUpdateListener {
        val scale = it.animatedValue as Float
        val newHeight = ((to - from) * scale + from).toInt()
        setClippedHeight(newHeight)
      }
      doOnEnd { onEnd() }
      start()
    }
  }

  private fun enqueueAnimation(action: () -> Unit) {
    if (!animator.isRunning) {
      action()
    } else {
      animator.doOnEnd { action() }
    }
  }

  private fun setClippedHeight(newHeight: Int) {
    if (newHeight <= maxPeekSize || maxPeekSize == 0) {
      behavior.peekHeight = newHeight
    }
  }

  private fun isLandscape(window: Window): Boolean {
    val view = window.decorView
    return view.width >= view.height
  }

  private fun updateBlurAndDimFraction(fraction: Float) {
    if (!supportsBlur) return
    val window = dialogWindow ?: return
    val clamped = fraction.coerceIn(0f, 1f)
    val targetRadius = (maxBlurRadius * clamped).roundToInt()
    val targetDimAmount = maxDimAmount * clamped.coerceAtLeast(0.1f)

    val layoutParams = window.attributes
    if (layoutParams.blurBehindRadius == targetRadius && layoutParams.dimAmount == targetDimAmount) return
    layoutParams.blurBehindRadius = targetRadius
    layoutParams.dimAmount = targetDimAmount
    window.attributes = layoutParams
  }

  private fun updateBlurAndDimForOffset(offset: Float) {
    if (offset.isNaN()) return
    val normalized = ((offset + 1f) / 1f).coerceIn(0f, 1f)
    updateBlurAndDimFraction(normalized)
  }

  private fun resetBlurRadius() {
    updateBlurAndDimFraction(0f)
  }
}
