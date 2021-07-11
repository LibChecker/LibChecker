package com.absinthe.libchecker

import android.content.pm.PackageManager

object SystemServices {
    val packageManager: PackageManager by lazy { LibCheckerApp.app.packageManager }
}
