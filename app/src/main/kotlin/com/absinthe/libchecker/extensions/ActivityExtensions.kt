package com.absinthe.libchecker.extensions

import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.constant.GlobalValues

fun BaseActivity.finishCompat() {
    if (GlobalValues.isShowEntryAnimation.valueUnsafe) {
        supportFinishAfterTransition()
    } else {
        onBackPressed()
    }
}