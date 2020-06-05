package com.absinthe.libchecker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.absinthe.libchecker.constant.GlobalValues
import com.blankj.utilcode.util.ToastUtils

class PackageReceiver :BroadcastReceiver(){

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
            intent.action == Intent.ACTION_PACKAGE_REMOVED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            GlobalValues.shouldRequestChange = true
            ToastUtils.showShort("BR Received")
        }
    }

}