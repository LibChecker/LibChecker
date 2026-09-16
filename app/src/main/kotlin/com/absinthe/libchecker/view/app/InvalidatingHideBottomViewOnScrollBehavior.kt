package com.absinthe.libchecker.view.app

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView

@Suppress("DEPRECATION")
internal class InvalidatingHideBottomViewOnScrollBehavior(
  private val canScroll: () -> Boolean = { true }
) : HideBottomViewOnScrollBehavior<BottomNavigationView>() {

  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: BottomNavigationView,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int
  ): Boolean = canScroll() && super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)

  override fun onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: BottomNavigationView,
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int,
    consumed: IntArray
  ) {
    // A scroll accepted before the IME opened can still deliver callbacks.
    if (!canScroll()) return
    super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
  }

  override fun slideUp(child: BottomNavigationView, animate: Boolean) {
    super.slideUp(child, animate)
    invalidateParentDuringAnimation(child, animate)
  }

  override fun slideDown(child: BottomNavigationView, animate: Boolean) {
    if (!canScroll()) return
    super.slideDown(child, animate)
    invalidateParentDuringAnimation(child, animate)
  }

  private fun invalidateParentDuringAnimation(child: BottomNavigationView, animate: Boolean) {
    val parent = child.parent as? View ?: return
    if (animate) {
      child.animate().setUpdateListener { parent.invalidate() }
    } else {
      parent.invalidate()
    }
  }
}
