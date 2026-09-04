package com.absinthe.libchecker.ui.base

import android.view.View

interface IAppBarContainer {
  fun scheduleAppbarLiftingStatus(isLifted: Boolean)
  fun setBlurDesignEnabled(enabled: Boolean)
  fun setFloatingNavBarEnabled(enabled: Boolean)
  fun prepareAppbarContentInset(targetView: View)
  fun setLiftOnScrollTargetView(targetView: View)
}
