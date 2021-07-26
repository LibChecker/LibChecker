package com.absinthe.libchecker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.viewmodel.GET_INSTALL_APPS_RETRY_PERIOD
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SHOOT_CHANNEL_ID = "shoot_channel"
private const val SHOOT_NOTIFICATION_ID = 1
private const val SHOOT_SUCCESS_NOTIFICATION_ID = 2

class ShootService : LifecycleService() {

  private val builder by lazy { NotificationCompat.Builder(this, SHOOT_CHANNEL_ID) }
  private val notificationManager by lazy { NotificationManagerCompat.from(this) }
  private val configuration by lazy {
    Configuration(resources.configuration).apply {
      setLocale(GlobalValues.locale)
    }
  }
  private val gson = Gson()
  private val repository = Repositories.lcRepository
  private val listenerList = RemoteCallbackList<OnShootListener>()
  private var isSendingBroadcast = false

  @DelicateCoroutinesApi
  private val binder = object : IShootService.Stub() {
    override fun computeSnapshot(dropPrevious: Boolean) {
      Timber.i("computeSnapshot: dropPrevious = $dropPrevious")
      lifecycleScope.launch(Dispatchers.IO) { this@ShootService.computeSnapshots(dropPrevious) }
    }

    override fun registerOnShootOverListener(listener: OnShootListener?) {
      Timber.i("registerOnShootOverListener $listener")
      listener?.let { listenerList.register(listener) }
    }

    override fun unregisterOnShootOverListener(listener: OnShootListener?) {
      Timber.i("unregisterOnShootOverListener $listener")
      listenerList.unregister(listener)
    }
  }

  @DelicateCoroutinesApi
  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)
    return binder
  }

  override fun onCreate() {
    super.onCreate()
    showNotification()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    showNotification()
    return super.onStartCommand(intent, flags, startId)
  }

  private fun showNotification() {
    initBuilder()

    notificationManager.apply {
      if (LCAppUtils.atLeastO()) {
        val name = createConfigurationContext(configuration).resources
          .getString(R.string.channel_shoot)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(SHOOT_CHANNEL_ID, name, importance)
        createNotificationChannel(mChannel)
      }
      startForeground(SHOOT_NOTIFICATION_ID, builder.build())
    }
  }

  private fun notifyFinished(timestamp: Long) {
    if (!isSendingBroadcast) {
      isSendingBroadcast = true
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
      isSendingBroadcast = false
    }
  }

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

  private suspend fun computeSnapshots(dropPrevious: Boolean = false) {
    Timber.i("computeSnapshots: dropPrevious = $dropPrevious")
    GlobalValues.hasFinishedShoot = false
    notificationManager.cancel(SHOOT_SUCCESS_NOTIFICATION_ID)
    initBuilder()

    val timer = TimeRecorder()
    timer.start()
    val ts = System.currentTimeMillis()

    var appList: List<ApplicationInfo>? = AppItemRepository.getApplicationInfoItems()

    if (appList.isNullOrEmpty()) {
      do {
        appList = try {
          PackageUtils.getInstallApplications()
        } catch (e: Exception) {
          delay(GET_INSTALL_APPS_RETRY_PERIOD)
          null
        }
      } while (appList == null)
    }

    repository.deleteAllSnapshotDiffItems()

    val size = appList.size
    var count = 0
    val dbList = mutableListOf<SnapshotItem>()
    val exceptionInfoList = mutableListOf<ApplicationInfo>()

    builder.setProgress(size, count, false)
    notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())

    for (info in appList) {
      try {
        PackageUtils.getPackageInfo(info.packageName).let {
          dbList.add(
            SnapshotItem(
              id = null,
              packageName = it.packageName,
              timeStamp = ts,
              label = info.loadLabel(packageManager).toString(),
              versionName = it.versionName ?: "null",
              versionCode = PackageUtils.getVersionCode(it),
              installedTime = it.firstInstallTime,
              lastUpdatedTime = it.lastUpdateTime,
              isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
              abi = PackageUtils.getAbi(info).toShort(),
              targetApi = info.targetSdkVersion.toShort(),
              nativeLibs = gson.toJson(
                PackageUtils.getNativeDirLibs(it)
              ),
              services = gson.toJson(
                PackageUtils.getComponentStringList(it.packageName, SERVICE, false)
              ),
              activities = gson.toJson(
                PackageUtils.getComponentStringList(it.packageName, ACTIVITY, false)
              ),
              receivers = gson.toJson(
                PackageUtils.getComponentStringList(it.packageName, RECEIVER, false)
              ),
              providers = gson.toJson(
                PackageUtils.getComponentStringList(it.packageName, PROVIDER, false)
              ),
              permissions = gson.toJson(PackageUtils.getPermissionsList(it.packageName))
            )
          )
        }
        count++
        notifyProgress(count * 100 / size)
      } catch (e: Exception) {
        Timber.e(e)
        exceptionInfoList.add(info)
        continue
      }

      if (dbList.size >= 50) {
        builder.setProgress(size, count, false)
        notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())
        repository.insertSnapshots(dbList)
        dbList.clear()
      }
    }

    var info: ApplicationInfo
    var abiValue: Int
    while (exceptionInfoList.isNotEmpty()) {
      try {
        info = exceptionInfoList[0]
        abiValue = PackageUtils.getAbi(info)
        PackageUtils.getPackageInfo(info.packageName).let {
          dbList.add(
            SnapshotItem(
              id = null,
              packageName = it.packageName,
              timeStamp = ts,
              label = info.loadLabel(packageManager).toString(),
              versionName = it.versionName ?: "null",
              versionCode = PackageUtils.getVersionCode(it),
              installedTime = it.firstInstallTime,
              lastUpdatedTime = it.lastUpdateTime,
              isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
              abi = abiValue.toShort(),
              targetApi = info.targetSdkVersion.toShort(),
              nativeLibs = gson.toJson(
                PackageUtils.getNativeDirLibs(it, PackageUtils.is32bit(abiValue))
              ),
              services = gson.toJson(
                PackageUtils.getComponentStringList(it.packageName, SERVICE, false)
              ),
              activities = gson.toJson(
                PackageUtils.getComponentStringList(it.packageName, ACTIVITY, false)
              ),
              receivers = gson.toJson(
                PackageUtils.getComponentStringList(it.packageName, RECEIVER, false)
              ),
              providers = gson.toJson(
                PackageUtils.getComponentStringList(it.packageName, PROVIDER, false)
              ),
              permissions = gson.toJson(PackageUtils.getPermissionsList(it.packageName))
            )
          )
        }
        exceptionInfoList.removeAt(0)
      } catch (e: Exception) {
        exceptionInfoList.removeAt(0)
        continue
      }
      count++
      notifyProgress(count * 100 / size)
    }

    builder.setProgress(size, count, false)
    notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())
    repository.insertSnapshots(dbList)
    repository.insert(TimeStampItem(ts, null))

    if (dropPrevious) {
      Timber.i("deleteSnapshotsAndTimeStamp: ${GlobalValues.snapshotTimestamp}")
      repository.deleteSnapshotsAndTimeStamp(GlobalValues.snapshotTimestamp)
    }

    builder.setProgress(0, 0, false)
      .setOngoing(false)
      .setContentTitle(createConfigurationContext(configuration).resources.getString(R.string.noti_shoot_title_saved))
      .setContentText(getFormatDateString(ts))
    notificationManager.notify(SHOOT_SUCCESS_NOTIFICATION_ID, builder.build())

    timer.end()
    Timber.d("computeSnapshots: $timer")

    GlobalValues.snapshotTimestamp = ts
    GlobalValues.hasFinishedShoot = true
    notifyFinished(ts)
    stopForeground(true)
    stopSelf()
    Timber.i("computeSnapshots end")
  }

  private fun getFormatDateString(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
  }

  private fun initBuilder() {
    val pi = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      PendingIntent.FLAG_IMMUTABLE
    )
    builder.setContentTitle(createConfigurationContext(configuration).resources.getString(R.string.noti_shoot_title))
      .setSmallIcon(R.drawable.ic_logo)
      .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
      .setColor(R.color.colorPrimary.getColor(this))
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(pi)
      .setProgress(0, 0, true)
      .setSilent(true)
      .setOngoing(true)
      .setAutoCancel(false)
  }
}
