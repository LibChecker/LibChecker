package com.absinthe.libchecker.features.applist.detail

import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

abstract class AppBarStateChangeListener : AppBarLayout.OnOffsetChangedListener {
  enum class State {
    EXPANDED,
    COLLAPSED,
    IDLE
  }

  private var mCurrentState = State.IDLE
  override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
    mCurrentState = when {
      i == 0 -> {
        if (mCurrentState != State.EXPANDED) {
          onStateChanged(appBarLayout, State.EXPANDED)
        }
        State.EXPANDED
      }

      abs(i) >= appBarLayout.totalScrollRange -> {
        if (mCurrentState != State.COLLAPSED) {
          onStateChanged(appBarLayout, State.COLLAPSED)
        }
        State.COLLAPSED
      }

      else -> {
        if (mCurrentState != State.IDLE) {
          onStateChanged(appBarLayout, State.IDLE)
        }
        State.IDLE
      }
    }
  }

  abstract fun onStateChanged(appBarLayout: AppBarLayout, state: State)
}
