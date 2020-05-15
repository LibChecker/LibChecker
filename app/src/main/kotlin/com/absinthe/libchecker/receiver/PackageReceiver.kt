package com.absinthe.libchecker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.viewmodel.AppViewModel

class PackageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
            intent.action == Intent.ACTION_PACKAGE_REMOVED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            ActivityStackManager.topActivity?.let {
                if (it is MainActivity) {
                    val viewModel = ViewModelProvider(it).get(AppViewModel::class.java)
                    viewModel.requestChange(it)
                }
            }
        }
    }

}