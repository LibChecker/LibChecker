package com.absinthe.libchecker

import android.content.Context
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager

object SystemServices {
  val packageManager: PackageManager by lazy { LibCheckerApp.app.packageManager }
  val inputMethodManager: InputMethodManager by lazy { LibCheckerApp.app.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
}
