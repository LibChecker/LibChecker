package com.absinthe.libchecker.ui.base

import android.content.SharedPreferences
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AbsListView
import android.widget.ScrollView
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.view.app.PROGRESSIVE_BLUR_RADIUS_DP
import com.absinthe.libchecker.view.app.ProgressiveBlurEffect

internal fun installDialogScrollBlur(root: View) {
  if (!OsUtils.atLeastT()) return
  val controller = DialogScrollBlur(root)
  root.addOnAttachStateChangeListener(controller)
  if (root.isAttachedToWindow) controller.onViewAttachedToWindow(root)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class DialogScrollBlur(private val root: View) :
  View.OnAttachStateChangeListener,
  SharedPreferences.OnSharedPreferenceChangeListener {
  private var enabled = false
  private val targets = mutableMapOf<View, EdgeState>()
  private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { discoverTargets() }
  private val drawListener = ViewTreeObserver.OnPreDrawListener {
    for ((view, state) in targets) {
      val top = view.isShown && view.canScrollVertically(-1)
      val bottom = view.isShown && view.canScrollVertically(1)
      state.update(view, top, bottom)
    }
    true
  }

  override fun onViewAttachedToWindow(v: View) {
    if (!root.isHardwareAccelerated) return
    SPUtils.sp.registerOnSharedPreferenceChangeListener(this)
    setEnabled(GlobalValues.isBlurDesign)
  }

  override fun onViewDetachedFromWindow(v: View) {
    SPUtils.sp.unregisterOnSharedPreferenceChangeListener(this)
    setEnabled(false)
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
    if (key == Constants.PREF_BLUR_DESIGN || key == null) setEnabled(GlobalValues.isBlurDesign)
  }

  private fun setEnabled(value: Boolean) {
    if (enabled == value) return
    enabled = value
    if (enabled) {
      discoverTargets()
      root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
      root.viewTreeObserver.addOnPreDrawListener(drawListener)
      root.invalidate()
    } else {
      root.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
      root.viewTreeObserver.removeOnPreDrawListener(drawListener)
      targets.forEach { (view, state) -> state.update(view, false, false) }
      targets.clear()
    }
  }

  private fun discoverTargets() {
    targets.entries.removeAll { (view, state) ->
      if (!view.isAttachedToWindow) state.update(view, false, false)
      !view.isAttachedToWindow
    }
    fun visit(view: View) {
      if (view is ScrollView || view is NestedScrollView || view is AbsListView || view is RecyclerView) {
        targets.getOrPut(view) { EdgeState() }
      } else if (view is ViewGroup) {
        for (index in 0 until view.childCount) visit(view.getChildAt(index))
      }
    }
    visit(root)
  }

  private class EdgeState {
    private var effectHost: View? = null
    private var blur: ProgressiveBlurEffect? = null
    private var top = false
    private var bottom = false
    private var height = 0

    fun update(view: View, newTop: Boolean, newBottom: Boolean) {
      // Android stretches the scroller's RenderNode after its effect. Composite on
      // the fixed viewport when it contains only the scroller, so the fade stays put.
      val parent = view.parent as? ViewGroup
      val host = parent?.takeIf {
        it.childCount == 1 && it.scrollX == 0 && it.scrollY == 0 &&
          view.x == 0f && view.y == 0f && it.width == view.width && it.height == view.height
      } ?: view
      if ((!newTop && !newBottom) || view.height == 0) {
        effectHost?.setRenderEffect(null)
        effectHost = null
        return
      }
      if (host === effectHost && height == view.height && top == newTop && bottom == newBottom) return
      if (host !== effectHost) effectHost?.setRenderEffect(null)
      effectHost = host
      top = newTop
      bottom = newBottom
      height = view.height

      val density = view.resources.displayMetrics.density
      val band = minOf(40f * density, height / 2f)
      val effect = blur ?: ProgressiveBlurEffect(PROGRESSIVE_BLUR_RADIUS_DP * density).also { blur = it }
      host.setRenderEffect(
        effect.create(
          start = if (top) 0f else -band,
          end = if (top) band else 0f,
          otherStart = if (bottom) height.toFloat() else height + band,
          otherEnd = if (bottom) height - band else height.toFloat(),
          blendIntoSurface = true
        )
      )
    }
  }
}
