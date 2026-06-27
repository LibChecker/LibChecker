package com.absinthe.libchecker.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.RemoteCallbackList
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.CaptureInstalledSnapshotUseCase
import com.absinthe.libchecker.domain.snapshot.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libraries.utils.manager.TimeRecorder
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

private const val SHOOT_CHANNEL_ID = "shoot_channel"

const val ACTION_SHOOT_AND_STOP_AUTO = "action_shoot_and_stop_auto"
const val EXTRA_DROP_PREVIOUS = "extra_drop_previous"

class ShootService : LifecycleService() {

  private val notificationIdShoot = Process.myPid()
  private val notificationIdShootSuccess = notificationIdShoot + 1
  private val builder by lazy { NotificationCompat.Builder(this, SHOOT_CHANNEL_ID) }
  private val notificationManager by lazy { NotificationManagerCompat.from(this) }
  private val installedAppRepository: InstalledAppRepository by inject()
  private val captureInstalledSnapshot: CaptureInstalledSnapshotUseCase by inject()
  private val formatSnapshotTimestamp: FormatSnapshotTimestampUseCase by inject()
  private val listenerList = RemoteCallbackList<OnShootListener>()

  private val binder by lazy { ShootBinder(this) }

  private var _isShooting: Boolean = false
  private var areNotificationsEnabled = false

  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)
    Timber.d("onBind")
    return binder
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.d("onStartCommand: ${intent?.action}")
    when (intent?.action) {
      ACTION_SHOOT_AND_STOP_AUTO -> {
        val dropPrevious = intent.getBooleanExtra(EXTRA_DROP_PREVIOUS, false)
        computeSnapshots(dropPrevious, true)
      }
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    Timber.d("onDestroy")
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
  }

  override fun onTimeout(startId: Int) {
    super.onTimeout(startId)
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onTimeout(startId: Int, fgsType: Int) {
    super.onTimeout(startId, fgsType)
    // https://developer.android.com/about/versions/15/behavior-changes-15?hl=zh-cn#datasync-timeout
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun showNotification(): Boolean {
    initBuilder()

    return runCatching {
      if (OsUtils.atLeastO()) {
        val name = getString(R.string.channel_shoot)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(SHOOT_CHANNEL_ID, name, importance)
        notificationManager.createNotificationChannel(channel)
      }
      if (OsUtils.atLeastU()) {
        startForeground(
          notificationIdShoot,
          builder.build(),
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        )
      } else {
        startForeground(notificationIdShoot, builder.build())
      }
    }.onFailure {
      Timber.w(it, "Failed to start snapshot foreground service")
    }.isSuccess
  }

  @Synchronized
  private fun notifyFinished(timestamp: Long) {
    Timber.i("notifyFinished start")
    val count = listenerList.beginBroadcast()
    for (i in 0 until count) {
      try {
        Timber.i("notifyFinished $i")
        listenerList.getBroadcastItem(i).onShootFinished(timestamp)
      } catch (e: RemoteException) {
        Timber.e(e)
      }
    }
    listenerList.finishBroadcast()
  }

  @Synchronized
  private fun notifyProgress(progress: Int) {
    val count = listenerList.beginBroadcast()
    for (i in 0 until count) {
      try {
        listenerList.getBroadcastItem(i).onProgressUpdated(progress)
      } catch (e: RemoteException) {
        Timber.e(e)
      }
    }
    listenerList.finishBroadcast()
  }

  private fun computeSnapshots(dropPrevious: Boolean = false, stopWhenFinish: Boolean = false) = lifecycleScope.launch(Dispatchers.IO) {
    computeSnapshotsImpl(installedAppRepository.getApplicationList(true), dropPrevious, stopWhenFinish)
  }

  private suspend fun computeSnapshotsImpl(appList: List<PackageInfo>, dropPrevious: Boolean = false, stopWhenFinish: Boolean = false) {
    if (isComputing) {
      Timber.w("computeSnapshots isComputing, ignored")
      return
    }
    isComputing = true
    Timber.i("computeSnapshots: dropPrevious = $dropPrevious")
    _isShooting = true

    val notificationPermissionGranted = !OsUtils.atLeastT() ||
      ContextCompat.checkSelfPermission(
        this@ShootService,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    val canPostNotification =
      notificationManager.areNotificationsEnabled() &&
        notificationPermissionGranted

    notificationManager.cancel(notificationIdShootSuccess)
    areNotificationsEnabled = showNotification() && canPostNotification

    if (areNotificationsEnabled) {
      builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
      notificationManager.notify(notificationIdShoot, builder.build())
    }

    val timer = TimeRecorder().also {
      it.start()
    }
    if (areNotificationsEnabled) {
      builder.setProgress(appList.size, 0, false)
      notificationManager.notify(notificationIdShoot, builder.build())
    }

    val result = captureInstalledSnapshot(
      CaptureInstalledSnapshotUseCase.Request(
        appList = appList,
        dropPrevious = dropPrevious,
        systemProps = getSystemProps()
      )
    ) { progress ->
      notifyProgress(progress.percent)
      if (areNotificationsEnabled) {
        builder.setProgress(progress.total, progress.count, false)
        notificationManager.notify(notificationIdShoot, builder.build())
      }
    }
    val timestamp = result.timestamp

    if (areNotificationsEnabled) {
      builder.setProgress(result.total, result.processedCount, false)
      notificationManager.notify(notificationIdShoot, builder.build())
      notificationManager.cancel(notificationIdShoot)

      builder.setProgress(0, 0, false)
        .setOngoing(false)
        .setContentTitle(getString(R.string.noti_shoot_title_saved))
        .setContentText(getFormatDateString(timestamp))
      notificationManager.notify(notificationIdShootSuccess, builder.build())
    }

    timer.end()
    Timber.d("computeSnapshots: $timer")

    _isShooting = false
    notifyFinished(timestamp)
    ServiceCompat.stopForeground(this@ShootService, ServiceCompat.STOP_FOREGROUND_REMOVE)
    Timber.i("computeSnapshots end")
    isComputing = false

    if (stopWhenFinish) {
      stopSelf()
    }
  }

  private fun getSystemProps(): Map<String, String> {
    return mapOf(
      Constants.SystemProps.RO_BUILD_VERSION_SECURITY_PATCH to Build.VERSION.SECURITY_PATCH,
      Constants.SystemProps.RO_BUILD_ID to Build.ID
    )
  }

  private fun getFormatDateString(timestamp: Long): String {
    return formatSnapshotTimestamp(timestamp)
  }

  private fun initBuilder() {
    val pi = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).also {
        it.action = Constants.ACTION_SNAPSHOT
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      },
      PendingIntent.FLAG_IMMUTABLE
    )
    builder.setContentTitle(getString(R.string.noti_shoot_title))
      .setSmallIcon(R.drawable.ic_logo)
      .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(pi)
      .setProgress(0, 0, true)
      .setSilent(true)
      .setOngoing(true)
      .setAutoCancel(false).apply {
        if (!OsUtils.atLeastS()) {
          color = getNotificationColor()
        }
      }
  }

  private fun getNotificationColor(): Int {
    return runCatching {
      getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
    }.getOrElse {
      Timber.w(it, "Failed to resolve notification color")
      ContextCompat.getColor(this, R.color.md_theme_libchecker_light_primary)
    }
  }

  companion object {
    var isComputing = false
  }

  class ShootBinder(service: ShootService) : IShootService.Stub() {

    private val serviceRef: WeakReference<ShootService> = WeakReference(service)

    override fun computeSnapshot(dropPrevious: Boolean) {
      Timber.i("computeSnapshot: dropPrevious = $dropPrevious")
      serviceRef.get()?.computeSnapshots(dropPrevious)
    }

    override fun isShooting(): Boolean {
      return serviceRef.get()?._isShooting == true
    }

    override fun registerOnShootOverListener(listener: OnShootListener?) {
      Timber.i("registerOnShootOverListener $listener")
      listener?.let {
        serviceRef.get()?.listenerList?.register(it)
      }
    }

    override fun unregisterOnShootOverListener(listener: OnShootListener?) {
      Timber.i("unregisterOnShootOverListener $listener")
      serviceRef.get()?.listenerList?.unregister(listener)
    }
  }
}
