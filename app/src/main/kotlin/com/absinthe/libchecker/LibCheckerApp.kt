package com.absinthe.libchecker

import android.app.Application
import com.absinthe.libchecker.constant.GlobalValues
import jonathanfinerty.once.Once

class LibCheckerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        GlobalValues.init(this)
        Once.initialise(this)
    }
}