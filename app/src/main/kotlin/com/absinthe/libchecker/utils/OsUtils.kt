package com.absinthe.libchecker.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object OsUtils {
  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
  fun atLeastT(): Boolean {
    return Build.VERSION.SDK_INT >= 33
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
  fun atLeastS(): Boolean {
    return Build.VERSION.SDK_INT >= 31
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
  fun atLeastR(): Boolean {
    return Build.VERSION.SDK_INT >= 30
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
  fun atLeastQ(): Boolean {
    return Build.VERSION.SDK_INT >= 29
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
  fun atLeastP(): Boolean {
    return Build.VERSION.SDK_INT >= 28
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
  fun atLeastO(): Boolean {
    return Build.VERSION.SDK_INT >= 26
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
  fun atLeastN(): Boolean {
    return Build.VERSION.SDK_INT >= 24
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
  fun atLeastM(): Boolean {
    //noinspection ObsoleteSdkInt
    return Build.VERSION.SDK_INT >= 23
  }
}
