package com.absinthe.libchecker.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.isOrientationLandscape
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

abstract class BaseBottomSheetViewDialogFragment<T : View> : BottomSheetDialogFragment() {

  private var _root: T? = null
  private var isHandlerActivated = false
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

          if (LCAppUtils.atLeastS()) {
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

    if (requireActivity().isOrientationLandscape) {
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

  override fun onDestroyView() {
    _root = null
    super.onDestroyView()
  }

  override fun show(manager: FragmentManager, tag: String?) {
    runCatching {
      super.show(manager, tag)
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
}
