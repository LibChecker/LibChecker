package com.absinthe.libchecker.data.app.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Toasty

private const val APP_UPDATE_CHANNEL_ID = "app_update_channel"
private const val APP_UPDATE_SUCCESS_NOTIFICATION_ID = 0x4c43

class AppUpdateInstallResultReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
    when (status) {
      PackageInstaller.STATUS_SUCCESS -> {
        Toasty.showLong(context, R.string.toast_app_update_installed)
        showInstallSuccessNotification(context)
      }

      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        val confirmationIntent = getPendingUserActionIntent(intent)
        if (confirmationIntent == null) {
          Toasty.showLong(context, R.string.toast_app_update_pending_user_action)
          return
        }
        confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
          context.startActivity(confirmationIntent)
        }.onFailure {
          Toasty.showLong(context, R.string.toast_app_update_pending_user_action)
        }
      }

      else -> {
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: status.toString()
        Toasty.showLong(context, context.getString(R.string.toast_app_update_failed, message))
      }
    }
  }

  companion object {
    const val EXTRA_SESSION_ID = "session_id"

    private fun getPendingUserActionIntent(intent: Intent): Intent? {
      return IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT)
    }

    private fun createInstallSuccessNotification(context: Context): Notification {
      val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      return NotificationCompat.Builder(context, APP_UPDATE_CHANNEL_ID)
        .setContentTitle(context.getString(R.string.toast_app_update_installed))
        .setSmallIcon(R.drawable.ic_logo)
        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    }

    @SuppressLint("MissingPermission")
    private fun showInstallSuccessNotification(context: Context) {
      val notificationManager = NotificationManagerCompat.from(context)
      ensureInstallSuccessNotificationChannel(context, notificationManager)
      if (!canPostNotifications(context, notificationManager)) {
        return
      }

      runCatching {
        notificationManager.notify(
          APP_UPDATE_SUCCESS_NOTIFICATION_ID,
          createInstallSuccessNotification(context)
        )
      }
    }

    private fun ensureInstallSuccessNotificationChannel(
      context: Context,
      notificationManager: NotificationManagerCompat
    ) {
      if (!OsUtils.atLeastO()) {
        return
      }

      val channel = NotificationChannel(
        APP_UPDATE_CHANNEL_ID,
        context.getString(R.string.settings_get_updates),
        NotificationManager.IMPORTANCE_DEFAULT
      )
      notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(
      context: Context,
      notificationManager: NotificationManagerCompat
    ): Boolean {
      val permissionGranted = !OsUtils.atLeastT() ||
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

      return notificationManager.areNotificationsEnabled() && permissionGranted
    }
  }
}
