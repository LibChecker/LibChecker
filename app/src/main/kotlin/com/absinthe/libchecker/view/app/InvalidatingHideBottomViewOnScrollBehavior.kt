package com.absinthe.libchecker.view.app

import android.view.View
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView

@Suppress("DEPRECATION")
internal class InvalidatingHideBottomViewOnScrollBehavior : HideBottomViewOnScrollBehavior<BottomNavigationView>() {

  override fun slideUp(child: BottomNavigationView, animate: Boolean) {
    super.slideUp(child, animate)
    invalidateParentDuringAnimation(child, animate)
  }

  override fun slideDown(child: BottomNavigationView, animate: Boolean) {
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
