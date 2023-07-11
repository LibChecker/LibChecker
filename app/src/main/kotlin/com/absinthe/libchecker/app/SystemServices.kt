package com.absinthe.libchecker.app

import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.absinthe.libchecker.LibCheckerApp

object SystemServices {
  val packageManager: PackageManager by lazy { LibCheckerApp.app.packageManager }
  val inputMethodManager: InputMethodManager by lazy { LibCheckerApp.app.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
  val windowManager: WindowManager by lazy { LibCheckerApp.app.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
}
