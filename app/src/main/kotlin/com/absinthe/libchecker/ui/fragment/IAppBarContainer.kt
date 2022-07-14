package com.absinthe.libchecker.ui.fragment

import android.view.View

interface IAppBarContainer {
  fun scheduleAppbarLiftingStatus(isLifted: Boolean, from: String)
  fun setLiftOnScrollTargetView(targetView: View)
}
