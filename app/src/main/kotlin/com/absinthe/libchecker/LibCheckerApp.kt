package com.absinthe.libchecker

import android.app.Application
import com.absinthe.libchecker.utils.GlobalValues

class LibCheckerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        GlobalValues.init(this)
    }
}