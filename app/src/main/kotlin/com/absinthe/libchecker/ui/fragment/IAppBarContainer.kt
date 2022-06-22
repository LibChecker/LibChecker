package com.absinthe.libchecker.ui.fragment

interface IAppBarContainer {
  fun scheduleAppbarLiftingStatus(isLifted: Boolean, from: String)
  fun bringAppbarToFront()
}
