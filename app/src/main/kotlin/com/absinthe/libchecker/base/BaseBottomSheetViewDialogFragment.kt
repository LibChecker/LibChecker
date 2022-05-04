package com.absinthe.libchecker.base

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.animation.doOnEnd
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.isOrientationLandscape
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libraries.utils.utils.UiUtils
import com.absinthe.libraries.utils.view.HeightClipDrawable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import timber.log.Timber

abstract class BaseBottomSheetViewDialogFragment<T : View> : BottomSheetDialogFragment(),
  View.OnLayoutChangeListener {

  var animationDuration = 350L

  private var _root: T? = null
  private var isHandlerActivated = false
  private var clipBounds2: Rect? = null // Because View#clipBounds creates a new Rect on every call.
  private var animator: ValueAnimator = ObjectAnimator()
  private var prevBlurRadius = 64
  private val behavior by unsafeLazy { BottomSheetBehavior.from(root.parent as View) }
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
        }
        BottomSheetBehavior.STATE_EXPANDED -> {
          if (isHandlerActivated) {
            isHandlerActivated = false
            getHeaderView().onHandlerActivated(false)
          }
          bottomSheet.background = createMaterialShapeDrawable(bottomSheet)
        }
        else -> {
        }
      }
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
      if (OsUtils.atLeastS()) {
        val blurRadius = (64 * (1 + slideOffset)).toInt()

        if (blurRadius != prevBlurRadius) {
          prevBlurRadius = blurRadius
          dialog?.window?.let {
            it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            it.attributes?.blurBehindRadius = prevBlurRadius
            it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
          }
        }
      }
    }
  }

  val root get() = _root!!

  abstract fun initRootView(): T
  abstract fun init()
  abstract fun getHeaderView(): BottomSheetHeaderView

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
    object : BottomSheetDialog(requireContext(), theme) {
      override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        window?.let {
          it.attributes?.windowAnimations = R.style.DialogAnimation
          WindowCompat.setDecorFitsSystemWindows(it, false)
          UiUtils.setSystemBarStyle(it)
          WindowInsetsControllerCompat(it, it.decorView)
            .isAppearanceLightNavigationBars = !UiUtils.isDarkMode()

          if (OsUtils.atLeastS()) {
            it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            it.attributes.blurBehindRadius = 64
            it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
          }
        }

        findViewById<View>(com.google.android.material.R.id.container)?.fitsSystemWindows = false
        findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
      }
    }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _root = initRootView()
    init()
    return _root
  }

  override fun onStart() {
    super.onStart()
    behavior.addBottomSheetCallback(bottomSheetCallback)
    root.addOnLayoutChangeListener(this)

    if (activity?.isOrientationLandscape == true) {
      root.post {
        Class.forName(behavior::class.java.name).apply {
          getDeclaredMethod("setStateInternal", Int::class.java).apply {
            isAccessible = true
            invoke(behavior, BottomSheetBehavior.STATE_EXPANDED)
          }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    behavior.removeBottomSheetCallback(bottomSheetCallback)
  }

  override fun onDetach() {
    animator.cancel()
    super.onDetach()
  }

  override fun onDestroyView() {
    root.removeOnLayoutChangeListener(this)
    _root = null
    super.onDestroyView()
  }

  override fun show(manager: FragmentManager, tag: String?) {
    runCatching {
      super.show(manager, tag)
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
      animateHeight(from = oldBottom - oldTop, to = bottom - top, onEnd = { })
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
      Timber.d("animateHeight: $from -> $to")

      addUpdateListener {
        val scale = it.animatedValue as Float
        val newHeight = ((to - from) * scale + from).toInt()
        setClippedHeight(newHeight)
      }
      doOnEnd { onEnd() }
      start()
    }
  }

  private fun setClippedHeight(newHeight: Int) {
    clipBounds2 = (clipBounds2 ?: Rect()).also {
      it.set(0, 0, root.right - root.left, root.top + newHeight)
    }
    (root.background as HeightClipDrawable?)?.clippedHeight = newHeight
    root.invalidate()
  }
}
