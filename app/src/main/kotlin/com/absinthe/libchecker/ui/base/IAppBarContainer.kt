package com.absinthe.libchecker.ui.base

import android.view.View

interface IAppBarContainer {
  fun scheduleAppbarLiftingStatus(isLifted: Boolean)
  fun setLiftOnScrollTargetView(targetView: View)
}
