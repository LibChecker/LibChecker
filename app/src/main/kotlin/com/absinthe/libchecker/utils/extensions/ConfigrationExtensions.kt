package com.absinthe.libchecker.utils.extensions

import android.content.res.Configuration

val Configuration.isOrientationLandscape: Boolean
  get() = orientation == Configuration.ORIENTATION_LANDSCAPE

val Configuration.isOrientationPortrait: Boolean
  get() = orientation == Configuration.ORIENTATION_PORTRAIT
