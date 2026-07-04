package com.absinthe.libchecker.data.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.Toasty

class AppUpdateInstallResultReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
      PackageInstaller.STATUS_SUCCESS -> {
        Toasty.showLong(context, R.string.toast_app_update_installed)
      }

      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        Toasty.showLong(context, R.string.toast_app_update_pending_user_action)
      }

      else -> {
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: status.toString()
        Toasty.showLong(context, context.getString(R.string.toast_app_update_failed, message))
      }
    }
  }

  companion object {
    const val EXTRA_SESSION_ID = "session_id"
  }
}
