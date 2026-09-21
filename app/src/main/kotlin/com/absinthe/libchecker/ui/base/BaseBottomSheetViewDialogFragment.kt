package com.absinthe.libchecker.ui.base

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.R as AppR
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.view.app.BottomSheetBackgroundDrawable
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.R
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import timber.log.Timber

abstract class BaseBottomSheetViewDialogFragment<T> :
  BottomSheetDialogFragment(),
  View.OnLayoutChangeListener where T : View, T : IHeaderView {

  var animationDuration = 350L
  var maxPeekHeightPercentage = 0f
  var maxPeekSize: Int = 0
  var isInitialLandscapeExpansionEnabled = true

  private var _root: T? = null
  private var isHandlerActivated = false
  private var isExternalHeightAnimationRunning = false
  private var animator: ValueAnimator = ObjectAnimator()
  private val supportsBlur = OsUtils.atLeastS()
  private val maxBlurRadius = 80f
  private val blurInterpolator = AccelerateDecelerateInterpolator()
  private var maxDimAmount: Float = 0f
  private var dialogWindow: Window? = null
  private var blurController: WindowBlurCompatController? = null
  private val behavior by lazy { BottomSheetBehavior.from(root.parent as View) }
  private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) {
      when (newState) {
        BottomSheetBehavior.STATE_DRAGGING -> {
          if (!isHandlerActivated) {
            isHandlerActivated = true
            root.getHeaderView().onHandlerActivated(true)
          }
        }

        BottomSheetBehavior.STATE_COLLAPSED -> {
          if (isHandlerActivated) {
            isHandlerActivated = false
            root.getHeaderView().onHandlerActivated(false)
          }
          updateBlurAndDimForOffset(0f)
        }

        BottomSheetBehavior.STATE_EXPANDED -> {
          if (isHandlerActivated) {
            isHandlerActivated = false
            root.getHeaderView().onHandlerActivated(false)
          }
          updateBlurAndDimForOffset(1f)
        }

        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
          updateBlurAndDimForOffset(0.5f)
        }

        BottomSheetBehavior.STATE_HIDDEN -> {
          finishBlurAnimation()
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
  protected open fun getHeaderContentSpacing(): Int {
    return resources.getDimensionPixelSize(AppR.dimen.bottom_sheet_header_content_spacing)
  }

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
          blurController?.stop()
          blurController = WindowBlurCompatController(requireActivity(), it).also { controller ->
            controller.start()
          }
          updateBlurAndDimFraction(1f, animateBlur = true)
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
    _root = initRootView().also { root ->
      root.post { updateMaxPeekSize() }
    }
    init()
    root.getHeaderView().title.updatePadding(
      bottom = getHeaderContentSpacing()
    )
    return _root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.setOnApplyWindowInsetsListener { _, insets ->
      ((view.parent as? View)?.background as? BottomSheetBackgroundDrawable)
        ?.updateCorners(view.rootWindowInsets)
      insets
    }
    view.viewTreeObserver.addOnGlobalLayoutListener(object :
      ViewTreeObserver.OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        activity?.window?.takeIf { isInitialLandscapeExpansionEnabled && isLandscape(it) }?.run {
          behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }
      }
    })
  }

  override fun onStart() {
    super.onStart()
    (root.parent as View).doOnLayout { bottomSheet ->
      // Install after Material's first layout, independently of its expanded-corner animation.
      val background = bottomSheet.background as? BottomSheetBackgroundDrawable
        ?: BottomSheetBackgroundDrawable(bottomSheet.context, bottomSheet.background as MaterialShapeDrawable)
          .also { bottomSheet.background = it }
      background.updateCorners(bottomSheet.rootWindowInsets)
    }
    updateMaxPeekSize()
    behavior.addBottomSheetCallback(bottomSheetCallback)
    root.addOnLayoutChangeListener(this)
  }

  override fun onStop() {
    super.onStop()
    behavior.removeBottomSheetCallback(bottomSheetCallback)
  }

  override fun onDetach() {
    animator.cancel()
    finishBlurAnimation()
    blurController = null
    dialogWindow = null
    super.onDetach()
  }

  override fun onDestroyView() {
    animator.cancel()
    root.removeOnLayoutChangeListener(this)
    finishBlurAnimation()
    blurController = null
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
    val height = bottom - top
    val oldHeight = oldBottom - oldTop
    if (height == oldHeight) return

    if (oldHeight == 0) {
      updateMaxPeekSize()
      setClippedHeight(height)
      return
    }

    if (isExternalHeightAnimationRunning) {
      setClippedHeight(height)
      return
    }

    enqueueAnimation {
      animateHeight(from = oldHeight, to = height, onEnd = { })
    }
  }

  protected fun setExternalHeightAnimationRunning(running: Boolean) {
    if (running) {
      animator.cancel()
    }
    isExternalHeightAnimationRunning = running
    if (root.isLaidOut) {
      setClippedHeight(root.height)
    }
  }

  private fun updateMaxPeekSize() {
    if (maxPeekHeightPercentage >= 0f) {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * maxPeekHeightPercentage).toInt()
    } else {
      throw IllegalArgumentException("maxPeekHeightPercentage must be greater than 0")
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
    val clippedHeight = if (maxPeekSize > 0) {
      newHeight.coerceAtMost(maxPeekSize)
    } else {
      newHeight
    }
    if (behavior.peekHeight != clippedHeight) {
      behavior.peekHeight = clippedHeight
    }
  }

  private fun isLandscape(window: Window): Boolean {
    val view = window.decorView
    return view.width >= view.height
  }

  private fun updateBlurAndDimFraction(fraction: Float, animateBlur: Boolean = false) {
    if (!supportsBlur) return
    val window = dialogWindow ?: return
    val clamped = fraction.coerceIn(0f, 1f)
    val targetRadius = maxBlurRadius * blurInterpolator.getInterpolation(clamped)
    val targetDimAmount = maxDimAmount * clamped.coerceAtLeast(0.1f)

    if (animateBlur) {
      blurController?.animateBlurRadius(targetRadius, animationDuration)
    } else {
      blurController?.setBlurRadius(targetRadius)
    }
    val layoutParams = window.attributes
    if (layoutParams.dimAmount == targetDimAmount) return
    layoutParams.dimAmount = targetDimAmount
    window.attributes = layoutParams
  }

  private fun updateBlurAndDimForOffset(offset: Float) {
    if (offset.isNaN()) return
    updateBlurAndDimFraction((offset + 1f).coerceIn(0f, 1f))
  }

  private fun finishBlurAnimation() {
    blurController?.finishWithAnimation(animationDuration)
  }
}
